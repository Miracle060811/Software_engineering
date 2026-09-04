package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.common.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates user supplied travel cities before AI generation. The model is never
 * treated as a source of truth for whether a place exists.
 */
@Service
public class TravelPlaceService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlaceService.class);
    private static final Object NOMINATIM_RATE_LOCK = new Object();
    private static final long NOMINATIM_MIN_INTERVAL_MS = 1100L;
    private static volatile long lastNominatimRequestAt;

    private static final Set<String> ACCEPTED_CITY_TYPES = Set.of("city", "town", "municipality");
    private static final Set<String> LOCAL_TRAVEL_CITIES = Set.of(
            "北京", "上海", "天津", "重庆", "广州", "深圳", "杭州", "南京", "苏州", "无锡", "常州",
            "成都", "西安", "武汉", "长沙", "郑州", "济南", "青岛", "厦门", "泉州", "福州", "合肥",
            "南昌", "昆明", "大理", "云南大理", "丽江", "三亚", "海口", "桂林", "南宁", "贵阳",
            "兰州", "西宁", "银川", "乌鲁木齐", "拉萨", "哈尔滨", "长春", "沈阳", "大连", "石家庄",
            "太原", "呼和浩特", "宁波", "温州", "徐州", "张家界", "珠海", "佛山", "东莞", "香港",
            "澳门", "台北", "东京", "大阪", "首尔", "新加坡", "曼谷", "巴黎", "伦敦", "纽约", "洛杉矶");

    private static final Pattern INVALID_PLACE_CHARACTERS = Pattern.compile("[\\p{Cntrl}{}<>\\[\\]`$\\\\]");
    private static final Pattern INSTRUCTION_LIKE_PLACE = Pattern.compile(
            "(?i)(忽略|系统提示|developer|assistant|prompt|指令|输出json|工具调用|执行命令)");
    private static final Pattern AFTER_TRAVEL_VERB = Pattern.compile(
            "(?:想|计划|准备|打算|准备好)?(?:去|到|前往|目的地(?:是|为|：|:)?)[\\s]*([\\p{L}][\\p{L}\\p{N}·.'’\\-，, ]{0,28}?)(?=旅游|旅行|游玩|玩|度假|出差|的?天气|的?酒店|的?景点|的?路线|的?行程|怎么|[。！？?!]|$)");
    private static final Pattern BEFORE_TRAVEL_TOPIC = Pattern.compile(
            "([\\p{L}][\\p{L}\\p{N}·.'’\\- ]{1,23}?)(?:有(?:哪些)?|哪里有|的)?(?:天气|酒店|景点|路线|行程|怎么玩)");
    private static final Pattern PLACE_ROUTE_QUESTION = Pattern.compile(
            "^([\\p{L}][\\p{L}\\p{N}·.'’\\- ]{1,29}?)(?:怎么去|如何前往|怎么走|能去吗|可以去吗|值得去吗|好玩吗)[？?。！!\\s]*$");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();


    @Value("${ai.place-verification.enabled:true}")
    private boolean onlineVerificationEnabled;

    @Value("${ai.place-verification.base-url:https://nominatim.openstreetmap.org/search}")
    private String verificationBaseUrl;

    @Value("${ai.place-verification.cache-hours:168}")
    private long cacheHours;

    @Value("${ai.place-verification.user-agent:TravelMate/1.0 (https://github.com/Miracle060811/Software_engineering)}")
    private String userAgent;

    public VerifiedPlace verifyCity(String rawInput, String fieldLabel) {
        String input = normalizePlaceInput(rawInput, fieldLabel);
        String key = comparisonKey(input);
        VerifiedPlace local = findLocalCity(input, key);

        if (!onlineVerificationEnabled) {
            if (local != null) {
                return local;
            }
            throw new TravelPlaceException("暂时无法核验“" + input + "”是否为可前往城市，请稍后重试");
        }

        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return resolveLookup(input, local, cached.lookup());
        }

        try {
            PlaceLookup lookup = queryNominatim(input);
            cache.put(key, new CacheEntry(lookup, Instant.now().plus(Duration.ofHours(Math.max(cacheHours, 1)))));
            return resolveLookup(input, local, lookup);
        } catch (TravelPlaceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("联网核验城市失败 [{}]: {}",
                    LogSanitizer.singleLine(input), LogSanitizer.singleLine(ex.getMessage()));
            if (local != null) {
                return local;
            }
            throw new TravelPlaceException("暂时无法核验“" + input + "”是否为可前往城市，请稍后重试");
        }
    }

    public void requireDifferentCities(VerifiedPlace origin, VerifiedPlace destination) {
        if (origin == null || destination == null) {
            return;
        }
        if (comparisonKey(origin.canonicalName()).equals(comparisonKey(destination.canonicalName()))) {
            throw new TravelPlaceException("出发地和目的地不能是同一个城市");
        }
    }

    public String findExplicitTravelCity(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        for (Pattern pattern : List.of(AFTER_TRAVEL_VERB, BEFORE_TRAVEL_TOPIC, PLACE_ROUTE_QUESTION)) {
            Matcher matcher = pattern.matcher(message.trim());
            if (matcher.find()) {
                String candidate = stripConversationalPrefix(matcher.group(1));
                if (candidate.length() >= 2 && candidate.length() <= 30) {
                    return candidate;
                }
            }
        }
        return null;
    }

    PlaceLookup parseNominatimResponse(String input, String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.isArray() || root.isEmpty()) {
            return PlaceLookup.notFound();
        }

        boolean sawNonCity = false;
        for (JsonNode result : root) {
            String type = result.path("addresstype").asText(result.path("type").asText("")).toLowerCase(Locale.ROOT);
            if (!ACCEPTED_CITY_TYPES.contains(type)) {
                sawNonCity = true;
                continue;
            }

            List<String> names = candidateNames(result);
            String matchedName = names.stream()
                    .filter(name -> placeNamesMatch(input, name))
                    .findFirst()
                    .orElse(null);
            if (matchedName == null) {
                continue;
            }

            String canonical = result.path("name").asText(matchedName);
            String displayName = result.path("display_name").asText(canonical);
            String countryCode = result.path("address").path("country_code").asText("").toUpperCase(Locale.ROOT);
            double latitude = parseCoordinate(result.path("lat").asText(""));
            double longitude = parseCoordinate(result.path("lon").asText(""));
            return PlaceLookup.found(new VerifiedPlace(input, canonical, displayName, countryCode,
                    latitude, longitude, "OpenStreetMap Nominatim"));
        }
        return sawNonCity ? PlaceLookup.nonCity() : PlaceLookup.notFound();
    }

    private PlaceLookup queryNominatim(String input) throws Exception {
        String url = normalizedVerificationBaseUrl()
                + "?format=jsonv2&addressdetails=1&namedetails=1&limit=5&layer=address&featureType=city&q="
                + URLEncoder.encode(input, StandardCharsets.UTF_8);

        synchronized (NOMINATIM_RATE_LOCK) {
            long waitMs = NOMINATIM_MIN_INTERVAL_MS - (System.currentTimeMillis() - lastNominatimRequestAt);
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            URI target = URI.create(url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(target)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.5")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = ExternalHttpClientFactory.create(target, Duration.ofSeconds(6))
                    .send(request, HttpResponse.BodyHandlers.ofString());
            lastNominatimRequestAt = System.currentTimeMillis();
            if (response.statusCode() != 200) {
                throw new IllegalStateException("地点服务 HTTP " + response.statusCode());
            }
            return parseNominatimResponse(input, response.body());
        }
    }

    private VerifiedPlace resolveLookup(String input, VerifiedPlace local, PlaceLookup lookup) {
        if (lookup.status() == LookupStatus.FOUND) {
            return lookup.place();
        }
        if (local != null) {
            return local;
        }
        if (lookup.status() == LookupStatus.NON_CITY) {
            throw new TravelPlaceException("“" + input + "”不是可用于行程规划的城市，无法生成路线");
        }
        throw new TravelPlaceException("未找到“" + input + "”对应的城市，请检查名称后重试");
    }

    private VerifiedPlace findLocalCity(String input, String key) {
        boolean known = LOCAL_TRAVEL_CITIES.stream().map(TravelPlaceService::comparisonKey).anyMatch(key::equals);
        return known ? new VerifiedPlace(input, canonicalLocalName(input), input, "", Double.NaN, Double.NaN,
                "TravelMate 内置城市目录") : null;
    }

    private String normalizePlaceInput(String rawInput, String fieldLabel) {
        String label = StringUtils.hasText(fieldLabel) ? fieldLabel : "地点";
        String input = rawInput == null ? "" : Normalizer.normalize(rawInput, Normalizer.Form.NFKC).trim();
        input = input.replaceAll("\\s+", " ");
        if (input.isBlank()) {
            throw new TravelPlaceException("请输入" + label);
        }
        if (input.length() < 2 || input.length() > 60) {
            throw new TravelPlaceException(label + "名称长度应在 2-60 个字符之间");
        }
        if (INVALID_PLACE_CHARACTERS.matcher(input).find() || INSTRUCTION_LIKE_PLACE.matcher(input).find()
                || input.startsWith("http://") || input.startsWith("https://")) {
            throw new TravelPlaceException(label + "格式不正确，请输入真实城市名称");
        }
        return input;
    }

    private List<String> candidateNames(JsonNode result) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        addName(names, result.path("name").asText(""));
        JsonNode address = result.path("address");
        for (String field : List.of("city", "town", "municipality", "county")) {
            addName(names, address.path(field).asText(""));
        }
        JsonNode namedetails = result.path("namedetails");
        if (namedetails.isObject()) {
            namedetails.fields().forEachRemaining(entry -> addName(names, entry.getValue().asText("")));
        }
        return new ArrayList<>(names);
    }

    private static void addName(Set<String> names, String name) {
        if (StringUtils.hasText(name)) {
            names.add(name.trim());
        }
    }

    private static boolean placeNamesMatch(String input, String candidate) {
        String inputKey = comparisonKey(input);
        String candidateKey = comparisonKey(candidate);
        if (inputKey.equals(candidateKey)) {
            return true;
        }
        String strippedInput = stripAdministrativeWords(inputKey);
        String strippedCandidate = stripAdministrativeWords(candidateKey);
        return strippedCandidate.length() >= 2
                && (strippedInput.equals(strippedCandidate)
                || strippedInput.endsWith(strippedCandidate)
                || strippedCandidate.endsWith(strippedInput));
    }

    private static String stripAdministrativeWords(String value) {
        String stripped = value.replaceAll("^(中国|中华人民共和国)", "")
                .replaceAll("^(北京市|上海市|天津市|重庆市)", "")
                .replaceAll("^(云南|浙江|江苏|四川|广东|福建|山东|陕西|湖南|湖北|河南|河北|海南|贵州|广西|安徽|江西|甘肃|青海|辽宁|吉林|黑龙江|山西)省?", "");
        return stripped.replaceAll("(特别行政区|自治州|自治县|地区|市|镇)$", "");
    }

    private static String comparisonKey(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s,，.。·'’\\-]", "");
    }

    private static String canonicalLocalName(String input) {
        String stripped = stripAdministrativeWords(comparisonKey(input));
        return stripped.isBlank() ? input : stripped;
    }

    private static String stripConversationalPrefix(String value) {
        return value == null ? "" : value.trim()
                .replaceFirst("^(我想问|我想知道|帮我查一下|帮我查|请问|推荐|介绍|查询|查一下)", "")
                .replaceFirst("^(我|我们|一家人|一个人|最近|下周|明天|后天|今年|暑假|寒假)+", "")
                .replaceFirst("^(想|计划|准备|打算)+", "")
                .trim();
    }

    private String normalizedVerificationBaseUrl() {
        String value = StringUtils.hasText(verificationBaseUrl)
                ? verificationBaseUrl.trim()
                : "https://nominatim.openstreetmap.org/search";
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private double parseCoordinate(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    public record VerifiedPlace(String input, String canonicalName, String displayName, String countryCode,
                                double latitude, double longitude, String source) {
        public boolean hasCoordinates() {
            return Double.isFinite(latitude) && Double.isFinite(longitude);
        }
    }

    enum LookupStatus { FOUND, NOT_FOUND, NON_CITY }

    record PlaceLookup(LookupStatus status, VerifiedPlace place) {
        static PlaceLookup found(VerifiedPlace place) { return new PlaceLookup(LookupStatus.FOUND, place); }
        static PlaceLookup notFound() { return new PlaceLookup(LookupStatus.NOT_FOUND, null); }
        static PlaceLookup nonCity() { return new PlaceLookup(LookupStatus.NON_CITY, null); }
    }

    private record CacheEntry(PlaceLookup lookup, Instant expiresAt) {
    }

    public static class TravelPlaceException extends RuntimeException {
        public TravelPlaceException(String message) {
            super(message);
        }
    }
}
