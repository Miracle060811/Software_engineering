package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelmate.common.Result;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.AiConsumedEventMapper;
import com.travelmate.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationEventController {
    private final AiConsumedEventMapper consumedEventMapper;
    private final NotificationMapper notificationMapper;
    private final String serviceToken;

    public InternalNotificationEventController(AiConsumedEventMapper consumedEventMapper,
                                               NotificationMapper notificationMapper,
                                               @Value("${app.internal-service-token}") String serviceToken) {
        this.consumedEventMapper = consumedEventMapper;
        this.notificationMapper = notificationMapper;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/events")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> consume(@RequestBody OutboxEnvelope envelope,
                                  @RequestHeader("X-Internal-Token") String token,
                                  @RequestHeader("Idempotency-Key") String idempotencyKey) {
        verifyToken(token);
        validateEnvelope(envelope, idempotencyKey);
        if (consumedEventMapper.insertIfAbsent(envelope.eventId(), envelope.eventType()) == 0) {
            return Result.success("事件已处理");
        }

        JsonNode payload = envelope.payload();
        Notification notification = new Notification();
        notification.setUserId(requiredLong(payload, "userId"));
        notification.setType(requiredText(payload, "type", 50));
        notification.setTitle(requiredText(payload, "title", 200));
        notification.setContent(optionalText(payload, "content", 4000));
        notification.setActionUrl(optionalText(payload, "link", 300));
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        if (notificationMapper.insert(notification) != 1) {
            throw new IllegalStateException("通知写入失败");
        }
        return Result.success("事件消费成功");
    }

    private void validateEnvelope(OutboxEnvelope envelope, String idempotencyKey) {
        if (envelope == null || envelope.eventId() == null || envelope.eventId().isBlank()
                || !envelope.eventId().equals(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "事件ID与幂等键不一致");
        }
        if (!"NotificationRequested".equals(envelope.eventType()) || envelope.payload() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的事件类型或空负载");
        }
    }

    private void verifyToken(String token) {
        boolean matched = token != null && MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        }
    }

    private Long requiredLong(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 无效");
        }
        return value.asLong();
    }

    private String requiredText(JsonNode payload, String field, int maxLength) {
        String value = optionalText(payload, field, maxLength);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 不能为空");
        }
        return value;
    }

    private String optionalText(JsonNode payload, String field, int maxLength) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText().trim();
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 超出长度限制");
        }
        return value;
    }

    public record OutboxEnvelope(String eventId, String eventType, JsonNode payload) {
    }
}
