package com.travelmate.microservices.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.integration.NotificationGateway;
import com.travelmate.mapper.LocalOutboxEventMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class LocalOutboxNotificationGateway implements NotificationGateway {
    private final LocalOutboxEventMapper mapper;
    private final ObjectMapper objectMapper;

    public LocalOutboxNotificationGateway(LocalOutboxEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(Long userId, String type, String title, String content, String link) {
        LocalDateTime now = LocalDateTime.now();
        LocalOutboxEvent event = new LocalOutboxEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setAggregateType(type);
        event.setAggregateId(String.valueOf(userId));
        event.setEventType("NotificationRequested");
        event.setPayload(toJson(new NotificationPayload(userId, type, title, content, link)));
        event.setStatus(0);
        event.setRetryCount(0);
        event.setNextRetryTime(now);
        event.setCreateTime(now);
        event.setUpdateTime(now);
        if (mapper.insert(event) != 1) {
            throw new IllegalStateException("本地生活通知事件写入 Outbox 失败");
        }
    }

    private String toJson(NotificationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("本地生活通知事件序列化失败", ex);
        }
    }

    private record NotificationPayload(Long userId, String type, String title, String content, String link) {
    }
}
