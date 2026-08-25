package com.travelmate.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

/** Uses Open-Meteo only for dates covered by its forecast window. */
@Service
public class TravelWeatherService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${ai.weather.enabled:true}")
    private boolean enabled;

    @Value("${ai.weather.base-url:https://api.open-meteo.com/v1/forecast}")
    private String baseUrl;

    public WeatherSnapshot getForecast(TravelPlaceService.VerifiedPlace place, LocalDate date) {
        if (!enabled) {
            throw new IllegalStateException("实时天气服务未启用");
        }
        if (place == null || !place.hasCoordinates()) {
            throw new IllegalStateException("该城市缺少可核验坐标，无法查询天气");
        }
        LocalDate today = LocalDate.now();
        if (date == null || date.isBefore(today) || date.isAfter(today.plusDays(15))) {
            throw new IllegalArgumentException("天气只支持查询今天起 16 天内的日期");
        }
        try {
            String response = fetch(place, date);
            return parseForecast(place, date, response);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("天气服务暂时不可用，请稍后重试");
        }
    }

    WeatherSnapshot parseForecast(TravelPlaceService.VerifiedPlace place, LocalDate date, String responseBody)
            throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode daily = root.path("daily");
        JsonNode times = daily.path("time");
        if (!times.isArray()) {
            throw new IllegalStateException("天气服务未返回日期数据");
        }
        int index = -1;
        for (int i = 0; i < times.size(); i++) {
            if (date.toString().equals(times.get(i).asText())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalStateException("天气服务未覆盖该日期");
        }
        int code = arrayInt(daily, "weather_code", index, -1);
        double max = arrayDouble(daily, "temperature_2m_max", index);
        double min = arrayDouble(daily, "temperature_2m_min", index);
        int rainProbability = arrayInt(daily, "precipitation_probability_max", index, -1);
        return new WeatherSnapshot(place.canonicalName(), date, weatherDescription(code), min, max,
                rainProbability, "Open-Meteo");
    }

    private String fetch(TravelPlaceService.VerifiedPlace place, LocalDate date) throws Exception {
        String url = normalizedBaseUrl()
                + "?latitude=" + place.latitude()
                + "&longitude=" + place.longitude()
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&timezone=auto&start_date=" + URLEncoder.encode(date.toString(), StandardCharsets.UTF_8)
                + "&end_date=" + URLEncoder.encode(date.toString(), StandardCharsets.UTF_8);
        URI target = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(target)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = ExternalHttpClientFactory.create(target, Duration.ofSeconds(6))
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("天气服务 HTTP " + response.statusCode());
        }
        return response.body();
    }

    private int arrayInt(JsonNode parent, String field, int index, int fallback) {
        JsonNode values = parent.path(field);
        return values.isArray() && index < values.size() && !values.get(index).isNull()
                ? values.get(index).asInt(fallback)
                : fallback;
    }

    private double arrayDouble(JsonNode parent, String field, int index) {
        JsonNode values = parent.path(field);
        if (!values.isArray() || index >= values.size() || values.get(index).isNull()) {
            throw new IllegalStateException("天气服务缺少温度数据");
        }
        return values.get(index).asDouble();
    }

    private String weatherDescription(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "晴间多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "有雨";
            case 71, 73, 75, 77, 85, 86 -> "有雪";
            case 95, 96, 99 -> "雷暴";
            default -> "天气状况待确认";
        };
    }

    private String normalizedBaseUrl() {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.open-meteo.com/v1/forecast";
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record WeatherSnapshot(String city, LocalDate date, String condition, double minTemperature,
                                  double maxTemperature, int precipitationProbability, String source) {
    }
}
