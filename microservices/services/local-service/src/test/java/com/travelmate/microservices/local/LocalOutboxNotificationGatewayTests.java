package com.travelmate.microservices.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.mapper.LocalOutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalOutboxNotificationGatewayTests {

    @Test
    void persistsNotificationRequestedEvent() {
        LocalOutboxEventMapper mapper = mock(LocalOutboxEventMapper.class);
        when(mapper.insert(org.mockito.ArgumentMatchers.any(LocalOutboxEvent.class))).thenReturn(1);
        LocalOutboxNotificationGateway gateway = new LocalOutboxNotificationGateway(mapper, new ObjectMapper());

        gateway.publish(9L, "hotel_order", "酒店支付成功", "订单已支付", "/hotels/orders/H1");

        ArgumentCaptor<LocalOutboxEvent> captor = ArgumentCaptor.forClass(LocalOutboxEvent.class);
        verify(mapper).insert(captor.capture());
        LocalOutboxEvent event = captor.getValue();
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType()).isEqualTo("NotificationRequested");
        assertThat(event.getAggregateId()).isEqualTo("9");
        assertThat(event.getPayload()).contains("hotel_order", "订单已支付");
        assertThat(event.getStatus()).isZero();
        assertThat(event.getRetryCount()).isZero();
    }
}
