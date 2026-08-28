package com.travelmate.microservices.traffic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.mapper.TrafficOutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.outbox.dispatcher.enabled", havingValue = "true")
public class TrafficOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TrafficOutboxDispatcher.class);

    private final TrafficOutboxEventMapper mapper;
    private final ObjectMapper objectMapper;
    private final RestClient notificationClient;
    private final String serviceToken;
    private final int batchSize;
    private final int maxRetries;
    private final int baseDelaySeconds;
    private final int claimTimeoutSeconds;

    public TrafficOutboxDispatcher(TrafficOutboxEventMapper mapper,
                                   ObjectMapper objectMapper,
                                   RestClient.Builder builder,
                                   @Value("${app.services.notification-url}") String notificationUrl,
                                   @Value("${app.internal-service-token}") String serviceToken,
                                   @Value("${app.outbox.dispatcher.batch-size:20}") int batchSize,
                                   @Value("${app.outbox.dispatcher.max-retries:8}") int maxRetries,
                                   @Value("${app.outbox.dispatcher.base-delay-seconds:5}") int baseDelaySeconds,
                                   @Value("${app.outbox.dispatcher.claim-timeout-seconds:60}") int claimTimeoutSeconds) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.notificationClient = builder.clone().baseUrl(notificationUrl).build();
        this.serviceToken = serviceToken;
        this.batchSize = Math.max(1, batchSize);
        this.maxRetries = Math.max(1, maxRetries);
        this.baseDelaySeconds = Math.max(1, baseDelaySeconds);
        this.claimTimeoutSeconds = Math.max(10, claimTimeoutSeconds);
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatcher.fixed-delay-ms:5000}")
    public void dispatchPending() {
        LocalDateTime now = LocalDateTime.now();
        mapper.releaseStaleClaims(now.minusSeconds(claimTimeoutSeconds));
        List<TrafficOutboxEvent> candidates = mapper.selectList(new LambdaQueryWrapper<TrafficOutboxEvent>()
                .eq(TrafficOutboxEvent::getStatus, 0)
                .le(TrafficOutboxEvent::getNextRetryTime, now)
                .orderByAsc(TrafficOutboxEvent::getId)
                .last("LIMIT " + batchSize));
        for (TrafficOutboxEvent event : candidates) {
            if (mapper.claim(event.getEventId()) != 1) {
                continue;
            }
            dispatchClaimed(event);
        }
    }

    private void dispatchClaimed(TrafficOutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            notificationClient.post()
                    .uri("/internal/notifications/events")
                    .header("X-Internal-Token", serviceToken)
                    .header("Idempotency-Key", event.getEventId())
                    .body(new OutboxEnvelope(event.getEventId(), event.getEventType(), payload))
                    .retrieve()
                    .toBodilessEntity();
            mapper.markPublished(event.getEventId(), LocalDateTime.now());
        } catch (Exception ex) {
            reschedule(event, ex);
        }
    }

    private void reschedule(TrafficOutboxEvent event, Exception ex) {
        int retryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        boolean dead = retryCount >= maxRetries;
        long multiplier = 1L << Math.min(retryCount - 1, 10);
        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(baseDelaySeconds * multiplier);
        mapper.markFailed(event.getEventId(), dead ? 2 : 0, retryCount, nextRetry, compactError(ex));
        log.warn("traffic_outbox_dispatch_failed eventId={} retry={} dead={}", event.getEventId(), retryCount, dead);
    }

    private String compactError(Exception ex) {
        String message = ex instanceof JsonProcessingException ? "invalid payload" : ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private record OutboxEnvelope(String eventId, String eventType, JsonNode payload) {
    }
}
