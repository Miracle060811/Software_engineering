package com.travelmate.microservices.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.mapper.TrafficOutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficOutboxNotificationGatewayTests {

    @Test
    void persistsNotificationRequestedEvent() {
        TrafficOutboxEventMapper mapper = mock(TrafficOutboxEventMapper.class);
        when(mapper.insert(org.mockito.ArgumentMatchers.any(TrafficOutboxEvent.class))).thenReturn(1);
        TrafficOutboxNotificationGateway gateway = new TrafficOutboxNotificationGateway(mapper, new ObjectMapper());

        gateway.publish(7L, "traffic_order", "出票成功", "订单已出票", "/orders/T1");

        ArgumentCaptor<TrafficOutboxEvent> captor = ArgumentCaptor.forClass(TrafficOutboxEvent.class);
        verify(mapper).insert(captor.capture());
        TrafficOutboxEvent event = captor.getValue();
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType()).isEqualTo("NotificationRequested");
        assertThat(event.getAggregateId()).isEqualTo("7");
        assertThat(event.getPayload()).contains("traffic_order", "订单已出票");
        assertThat(event.getStatus()).isZero();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryTime()).isNotNull();
    }

    @Test
    void rejectsFailedOutboxInsert() {
        TrafficOutboxEventMapper mapper = mock(TrafficOutboxEventMapper.class);
        when(mapper.insert(org.mockito.ArgumentMatchers.any(TrafficOutboxEvent.class))).thenReturn(0);
        TrafficOutboxNotificationGateway gateway = new TrafficOutboxNotificationGateway(mapper, new ObjectMapper());

        assertThatThrownBy(() -> gateway.publish(7L, "traffic_order", "标题", "内容", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outbox");
    }
}
