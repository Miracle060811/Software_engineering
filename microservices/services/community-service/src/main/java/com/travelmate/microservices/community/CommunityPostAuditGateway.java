package com.travelmate.microservices.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CommunityPostAuditGateway {
    private final RestClient aiClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public CommunityPostAuditGateway(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${app.services.ai-url}") String aiUrl,
                                     @Value("${app.internal-service-token}") String token) {
        this.aiClient = builder.clone().baseUrl(aiUrl).build();
        this.objectMapper = objectMapper;
        this.token = token;
    }

    public AuditDecision audit(String title, String content, String tags, String destination) {
        AuditDecision decision = aiClient.post().uri("/internal/ai/post-audit")
                .header("X-Internal-Token", token)
                .body(Map.of(
                        "title", safe(title),
                        "content", safe(content),
                        "tags", safe(tags),
                        "destination", safe(destination)))
                .retrieve().body(AuditDecision.class);
        return decision == null ? new AuditDecision(true, null) : decision;
    }

    public void notify(String eventId, Long userId, String title, String content, String link) {
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "type", "post_audit",
                "title", title,
                "content", content,
                "link", link);
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", "NotificationRequested",
                "payload", objectMapper.valueToTree(payload));
        aiClient.post().uri("/internal/notifications/events")
                .header("X-Internal-Token", token)
                .header("Idempotency-Key", eventId)
                .body(envelope).retrieve().toBodilessEntity();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record AuditDecision(boolean approved, String reason) {}
}
