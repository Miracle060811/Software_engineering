package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Reads public catalog data over HTTP, never the LOCAL database. */
@Component
public class AiTravelContextGateway {
    static final String UNAVAILABLE = "本地景点参考暂不可用，请使用真实常见景点并控制同日距离，不得编造票价或开放状态。";
    private final String localUrl;
    private final ObjectMapper mapper;

    public AiTravelContextGateway(@Value("${app.services.local-url:http://127.0.0.1:8083}") String localUrl,
                                  ObjectMapper mapper) {
        this.localUrl = localUrl.replaceAll("/+$", "");
        this.mapper = mapper;
    }

    public String attractions(String destination) {
        try {
            String city = destination.endsWith("市") ? destination.substring(0, destination.length() - 1) : destination;
            URI uri = URI.create(localUrl + "/api/attraction/search?city=" + URLEncoder.encode(city, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = ExternalHttpClientFactory.create(uri, Duration.ofSeconds(2))
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return UNAVAILABLE;
            JsonNode body = mapper.readTree(response.body());
            JsonNode data = body.path("data");
            if (body.path("code").asInt() != 200 || !data.isArray() || data.isEmpty()) return UNAVAILABLE;
            StringBuilder result = new StringBuilder("平台景点目录参考（非实时保证）：\n");
            int count = 0;
            for (JsonNode item : data) {
                if (++count > 8) break;
                result.append(item.path("name").asText()).append("，地址：")
                        .append(item.path("address").asText()).append("，开放时间参考：")
                        .append(item.path("openTime").asText()).append("，成人票参考：")
                        .append(item.path("adultPrice").asText()).append("\n");
            }
            return result.toString();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return UNAVAILABLE;
        } catch (Exception ex) {
            return UNAVAILABLE;
        }
    }
}
