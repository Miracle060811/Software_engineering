package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.AiConsumedEventMapper;
import com.travelmate.mapper.NotificationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalNotificationEventControllerTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateEventCreatesOnlyOneNotification() throws Exception {
        AiConsumedEventMapper consumedMapper = mock(AiConsumedEventMapper.class);
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        when(consumedMapper.insertIfAbsent("event-1", "NotificationRequested")).thenReturn(1, 0);
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);
        InternalNotificationEventController controller =
                new InternalNotificationEventController(consumedMapper, notificationMapper, "service-token");
        InternalNotificationEventController.OutboxEnvelope envelope = envelope("event-1");

        assertThat(controller.consume(envelope, "service-token", "event-1").getData()).isEqualTo("事件消费成功");
        assertThat(controller.consume(envelope, "service-token", "event-1").getData()).isEqualTo("事件已处理");

        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void rejectsInvalidInternalTokenBeforeWriting() throws Exception {
        AiConsumedEventMapper consumedMapper = mock(AiConsumedEventMapper.class);
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        InternalNotificationEventController controller =
                new InternalNotificationEventController(consumedMapper, notificationMapper, "service-token");

        assertThatThrownBy(() -> controller.consume(envelope("event-2"), "wrong", "event-2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verify(consumedMapper, never()).insertIfAbsent(any(), any());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void failsWhenNotificationCannotBePersisted() throws Exception {
        AiConsumedEventMapper consumedMapper = mock(AiConsumedEventMapper.class);
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        when(consumedMapper.insertIfAbsent("event-3", "NotificationRequested")).thenReturn(1);
        when(notificationMapper.insert(any(Notification.class))).thenReturn(0);
        InternalNotificationEventController controller =
                new InternalNotificationEventController(consumedMapper, notificationMapper, "service-token");

        assertThatThrownBy(() -> controller.consume(envelope("event-3"), "service-token", "event-3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("通知写入失败");
    }

    @Test
    void validNotificationEventIsAcceptedOverHttp() throws Exception {
        AiConsumedEventMapper consumedMapper = mock(AiConsumedEventMapper.class);
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        when(consumedMapper.insertIfAbsent("event-http-1", "NotificationRequested")).thenReturn(1);
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalNotificationEventController(consumedMapper, notificationMapper, "service-token")).build();
        mvc.perform(post("/internal/notifications/events")
                        .header("X-Internal-Token", "service-token")
                        .header("Idempotency-Key", "event-http-1")
                        .contentType("application/json")
                        .content("{\"eventId\":\"event-http-1\",\"eventType\":\"NotificationRequested\",\"payload\":{\"userId\":7,\"type\":\"system\",\"title\":\"通知\",\"content\":\"内容\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void invalidNotificationTokenIsRejectedOverHttp() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new InternalNotificationEventController(
                mock(AiConsumedEventMapper.class), mock(NotificationMapper.class), "service-token")).build();
        mvc.perform(post("/internal/notifications/events")
                        .header("X-Internal-Token", "wrong-token")
                        .header("Idempotency-Key", "event-http-2")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedNotificationEventIsRejected() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new InternalNotificationEventController(
                mock(AiConsumedEventMapper.class), mock(NotificationMapper.class), "service-token")).build();
        mvc.perform(post("/internal/notifications/events")
                        .header("X-Internal-Token", "service-token")
                        .header("Idempotency-Key", "event-http-3"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/internal/notifications/events")
                        .header("X-Internal-Token", "service-token")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    private InternalNotificationEventController.OutboxEnvelope envelope(String eventId) throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"userId":7,"type":"traffic_order","title":"出票成功","content":"订单已出票","link":"/orders/T1"}
                """);
        return new InternalNotificationEventController.OutboxEnvelope(eventId, "NotificationRequested", payload);
    }
}
