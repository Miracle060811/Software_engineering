package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class AiPostAuditService {
    private static final Logger log = LoggerFactory.getLogger(AiPostAuditService.class);
    private static final String SYSTEM_PROMPT = """
            你是 TravelMate 社区内容审核员。默认通过，只有在内容明确包含违法交易、暴力威胁、仇恨骚扰、色情招嫖、诈骗或泄露他人隐私时才拒绝。
            空白图片、短文本、普通抱怨、负面评价、玩笑、非旅行日常、疑似广告措辞、聚会或饮酒都不能单独构成拒绝理由。
            返回 JSON：{\"decision\":\"approve|reject\",\"reason\":\"20字以内中文原因\"}。
            """;
    private final RestClient opsClient;
    private final ObjectMapper objectMapper;
    private final String token;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;
    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${ai.deepseek.chat-completions-path:/chat/completions}")
    private String chatCompletionsPath;
    @Value("${ai.deepseek.chat-model:deepseek-v4-flash}")
    private String chatModel;

    public AiPostAuditService(RestClient.Builder builder, ObjectMapper objectMapper,
                              @Value("${app.services.ops-url}") String opsUrl,
                              @Value("${app.internal-service-token}") String token) {
        this.opsClient = builder.clone().baseUrl(opsUrl).build();
        this.objectMapper = objectMapper;
        this.token = token;
    }

    public AuditDecision audit(AuditRequest request) {
        String text = String.join("\n", safe(request.title()), safe(request.destination()),
                safe(request.tags()), safe(request.content()));
        AuditDecision fallback = opsClient.post().uri("/internal/ops/content/audit")
                .header("X-Internal-Token", token).body(Map.of("content", text))
                .retrieve().body(AuditDecision.class);
        if (fallback == null) fallback = new AuditDecision(true, null);
        if (apiKey == null || apiKey.isBlank() || "sk-demo-placeholder".equals(apiKey)) return fallback;
        try {
            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            messages.addObject().put("role", "user").put("content",
                    "标题：" + limited(request.title(), 200) + "\n目的地：" + limited(request.destination(), 100)
                            + "\n标签：" + limited(request.tags(), 300) + "\n正文：" + limited(request.content(), 4000));
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", resolveModel());
            body.set("messages", messages);
            body.putObject("response_format").put("type", "json_object");
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(resolveUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return fallback;
            String content = objectMapper.readTree(response.body()).path("choices").path(0)
                    .path("message").path("content").asText("");
            JsonNode decision = objectMapper.readTree(content);
            if ("approve".equalsIgnoreCase(decision.path("decision").asText())) return new AuditDecision(true, null);
            if ("reject".equalsIgnoreCase(decision.path("decision").asText())) {
                return new AuditDecision(false, limited(decision.path("reason").asText("AI自动审核"), 300));
            }
        } catch (Exception exception) {
            log.warn("AI 游记审核失败，使用敏感词等级规则降级: {}", exception.getMessage());
        }
        return fallback;
    }

    private String resolveUrl() {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/chat/completions")) return normalized;
        String path = chatCompletionsPath == null || chatCompletionsPath.isBlank()
                ? "/chat/completions" : chatCompletionsPath.trim();
        return normalized + (path.startsWith("/") ? path : "/" + path);
    }

    private String resolveModel() {
        String model = chatModel == null || chatModel.isBlank() ? "deepseek-chat" : chatModel.trim();
        return baseUrl != null && baseUrl.contains("api.deepseek.com") && model.startsWith("deepseek-v4")
                ? "deepseek-chat" : model;
    }

    private String limited(String value, int max) {
        String normalized = safe(value).trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record AuditRequest(String title, String content, String tags, String destination) {}
    public record AuditDecision(boolean approved, String reason) {}
}
