package com.travelmate.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.TrainBrowserTicket;
import com.travelmate.service.TrainBrowserSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

@Service
public class TrainBrowserSyncServiceImpl implements TrainBrowserSyncService {
    private static final String INIT_URL = "https://kyfw.12306.cn/otn/leftTicket/init";
    private static final String QUERY_URL = "https://kyfw.12306.cn/otn/leftTicket/query";
    private static final String STATION_URL =
            "https://kyfw.12306.cn/otn/resources/js/framework/station_name.js";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140 Safari/537.36";
    private static final Duration STATION_CATALOG_TTL = Duration.ofHours(24);

    private final ObjectMapper objectMapper;
    private final CookieManager cookieManager;
    private final HttpClient httpClient;
    private final Object sessionLock = new Object();

    private volatile boolean sessionReady;
    private volatile StationCatalog stationCatalog = StationCatalog.empty();
    private volatile LocalDateTime stationCatalogLoadedAt;

    // 外部 12306 服务不可用时必须尽快回退到本地车次数据，不能占满搜索接口超时时间。
    @Value("${train.browser-sync.timeout-ms:6000}")
    private long timeoutMs;

    public TrainBrowserSyncServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(cfcaSslContext())
                .build();
    }

    /**
     * 12306 当前证书链使用 CFCA EV ROOT，而部分 JDK（包括 Oracle JDK 25）
     * 未内置该根证书。这里只为访问 12306 的专用 HttpClient 增加该公开根证书，
     * 仍保留完整的证书链和主机名校验，不允许跳过 TLS 验证。
     */
    private SSLContext cfcaSslContext() {
        try (InputStream input = TrainBrowserSyncServiceImpl.class
                .getResourceAsStream("/certificates/cfca-ev-root.pem")) {
            if (input == null) {
                throw new IllegalStateException("缺少 12306 CFCA 根证书资源");
            }
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("cfca-ev-root", certificate);
            TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers.getTrustManagers(), null);
            return sslContext;
        } catch (Exception exception) {
            throw new IllegalStateException("无法初始化 12306 HTTPS 信任链", exception);
        }
    }

    @Override
    public List<TrainBrowserTicket> readPublicLeftTickets(
            String depStation,
            String depCode,
            String arrStation,
            String arrCode,
            LocalDate trainDate) throws Exception {
        ensureSession(false);

        String resolvedDepCode = StringUtils.hasText(depCode) ? depCode : stationCode(depStation);
        String resolvedArrCode = StringUtils.hasText(arrCode) ? arrCode : stationCode(arrStation);
        if (!StringUtils.hasText(resolvedDepCode) || !StringUtils.hasText(resolvedArrCode)) {
            throw new IllegalArgumentException("12306 未识别站点：" + depStation + " -> " + arrStation);
        }

        String responseBody = query(resolvedDepCode, resolvedArrCode, trainDate, false);
        return parseTickets(responseBody, depStation, arrStation);
    }

    @Override
    public List<String> resolveStationCandidates(String input) {
        String normalized = normalize(input);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        try {
            StationCatalog catalog = stationCatalog();
            List<String> cityStations = catalog.byCity().get(normalized);
            if (cityStations != null && !cityStations.isEmpty()) {
                return cityStations.stream()
                        .sorted(Comparator.comparingInt(name -> stationPriority(normalized, name)))
                        .toList();
            }
            if (catalog.byName().containsKey(normalized)) {
                return List.of(normalized);
            }
        } catch (Exception ignored) {
            // The resolver retains a local fallback list when 12306 is temporarily unreachable.
        }
        return List.of(normalized);
    }

    List<TrainBrowserTicket> parseTickets(String responseBody, String depStation, String arrStation) throws Exception {
        JsonNode root = objectMapper.readTree(stripBom(responseBody));
        if (root.path("httpstatus").asInt(200) != 200) {
            throw new IllegalStateException(responseMessage(root, "12306 查询失败"));
        }

        JsonNode data = root.path("data");
        JsonNode results = data.path("result");
        if (!results.isArray()) {
            throw new IllegalStateException(responseMessage(root, "12306 返回了无法识别的余票数据"));
        }

        Map<String, String> stationNames = new LinkedHashMap<>(stationCatalog.byCode());
        JsonNode responseMap = data.path("map");
        if (responseMap.isObject()) {
            responseMap.fields().forEachRemaining(entry -> stationNames.put(entry.getKey(), entry.getValue().asText()));
        }

        List<TrainBrowserTicket> tickets = new ArrayList<>();
        for (JsonNode result : results) {
            String[] fields = result.asText("").split("\\|", -1);
            if (fields.length < 33 || !StringUtils.hasText(field(fields, 3))) {
                continue;
            }

            TrainBrowserTicket ticket = new TrainBrowserTicket();
            ticket.setTrainNo(field(fields, 3));
            ticket.setDepartureStation(stationNames.getOrDefault(field(fields, 6), depStation));
            ticket.setArrivalStation(stationNames.getOrDefault(field(fields, 7), arrStation));
            ticket.setDepartureTime(field(fields, 8));
            ticket.setArrivalTime(field(fields, 9));
            ticket.setDuration(field(fields, 10));
            ticket.setBusinessSeat(firstSeat(fields, 32, 25));
            ticket.setFirstClassSeat(seat(fields, 31));
            ticket.setSecondClassSeat(seat(fields, 30));
            ticket.setHardSeat(seat(fields, 29));
            ticket.setNoSeat(seat(fields, 26));
            tickets.add(ticket);
        }
        return tickets;
    }

    private String query(String depCode, String arrCode, LocalDate trainDate, boolean retried) throws Exception {
        String url = QUERY_URL
                + "?leftTicketDTO.train_date=" + encode(trainDate.toString())
                + "&leftTicketDTO.from_station=" + encode(depCode)
                + "&leftTicketDTO.to_station=" + encode(arrCode)
                + "&purpose_codes=ADULT";
        HttpResponse<String> response = send(url, "application/json, text/javascript, */*; q=0.01");
        String body = stripBom(response.body());
        if (response.statusCode() >= 200 && response.statusCode() < 300 && body.trim().startsWith("{")) {
            return body;
        }
        if (!retried) {
            ensureSession(true);
            return query(depCode, arrCode, trainDate, true);
        }
        throw new IllegalStateException("12306 返回了维护页或访问校验页");
    }

    private String stationCode(String stationName) throws Exception {
        StationInfo station = stationCatalog().byName().get(normalize(stationName));
        return station == null ? "" : station.code();
    }

    private StationCatalog stationCatalog() throws Exception {
        StationCatalog current = stationCatalog;
        LocalDateTime loadedAt = stationCatalogLoadedAt;
        if (!current.byName().isEmpty() && loadedAt != null
                && loadedAt.plus(STATION_CATALOG_TTL).isAfter(LocalDateTime.now())) {
            return current;
        }

        synchronized (sessionLock) {
            current = stationCatalog;
            loadedAt = stationCatalogLoadedAt;
            if (!current.byName().isEmpty() && loadedAt != null
                    && loadedAt.plus(STATION_CATALOG_TTL).isAfter(LocalDateTime.now())) {
                return current;
            }
            ensureSession(false);
            HttpResponse<String> response = send(STATION_URL, "application/javascript, */*; q=0.01");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("12306 站点目录读取失败：HTTP " + response.statusCode());
            }
            StationCatalog parsed = parseStationCatalog(response.body());
            if (parsed.byName().isEmpty()) {
                throw new IllegalStateException("12306 站点目录为空");
            }
            stationCatalog = parsed;
            stationCatalogLoadedAt = LocalDateTime.now();
            return parsed;
        }
    }

    private StationCatalog parseStationCatalog(String script) {
        int start = script.indexOf('\'');
        int end = script.lastIndexOf('\'');
        if (start < 0 || end <= start) {
            return StationCatalog.empty();
        }

        Map<String, StationInfo> byName = new LinkedHashMap<>();
        Map<String, String> byCode = new LinkedHashMap<>();
        Map<String, List<String>> byCity = new LinkedHashMap<>();
        String stationData = script.substring(start + 1, end);
        for (String entry : stationData.split("@")) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length < 3 || !StringUtils.hasText(parts[1]) || !StringUtils.hasText(parts[2])) {
                continue;
            }
            String name = normalize(parts[1]);
            String code = parts[2].trim();
            String city = parts.length > 7 ? normalize(parts[7]) : "";
            StationInfo station = new StationInfo(name, code, city);
            byName.put(name, station);
            byCode.put(code, name);
            if (StringUtils.hasText(city)) {
                byCity.computeIfAbsent(city, ignored -> new ArrayList<>()).add(name);
            }
        }

        Map<String, List<String>> immutableCities = new LinkedHashMap<>();
        byCity.forEach((city, names) -> immutableCities.put(city, List.copyOf(names)));
        return new StationCatalog(
                Collections.unmodifiableMap(byName),
                Collections.unmodifiableMap(byCode),
                Collections.unmodifiableMap(immutableCities));
    }

    private void ensureSession(boolean forceRefresh) throws Exception {
        if (sessionReady && !forceRefresh) {
            return;
        }
        synchronized (sessionLock) {
            if (sessionReady && !forceRefresh) {
                return;
            }
            if (forceRefresh) {
                cookieManager.getCookieStore().removeAll();
                sessionReady = false;
            }
            HttpResponse<String> response = send(INIT_URL, "text/html,application/xhtml+xml");
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                throw new IllegalStateException("12306 页面初始化失败：HTTP " + response.statusCode());
            }
            sessionReady = true;
        }
    }

    private HttpResponse<String> send(String url, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(timeoutMs, 3000)))
                .header("User-Agent", USER_AGENT)
                .header("Accept", accept)
                .header("Referer", INIT_URL)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String responseMessage(JsonNode root, String fallback) {
        JsonNode messages = root.path("messages");
        if (messages.isArray() && !messages.isEmpty()) {
            return messages.get(0).asText(fallback);
        }
        return fallback;
    }

    private static String firstSeat(String[] fields, int... indexes) {
        for (int index : indexes) {
            String value = field(fields, index);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "--";
    }

    private static String seat(String[] fields, int index) {
        String value = field(fields, index);
        return StringUtils.hasText(value) ? value : "--";
    }

    private static String field(String[] fields, int index) {
        return index >= 0 && index < fields.length ? fields[index].trim() : "";
    }

    private static int stationPriority(String city, String station) {
        if (station.equals(city + "南")) return 0;
        if (station.equals(city + "东")) return 1;
        if (station.equals(city + "西")) return 2;
        if (station.equals(city + "北")) return 3;
        if (station.equals(city)) return 4;
        if (station.startsWith(city)) return 5;
        return 6;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().replace("　", "") : "";
    }

    private record StationInfo(String name, String code, String city) {
    }

    private record StationCatalog(
            Map<String, StationInfo> byName,
            Map<String, String> byCode,
            Map<String, List<String>> byCity) {
        private static StationCatalog empty() {
            return new StationCatalog(Map.of(), Map.of(), Map.of());
        }
    }
}
