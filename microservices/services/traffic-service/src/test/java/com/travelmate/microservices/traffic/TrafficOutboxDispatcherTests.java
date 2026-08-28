package com.travelmate.microservices.traffic;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.mapper.TrafficOutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TrafficOutboxDispatcherTests {

    @Test
    @SuppressWarnings("unchecked")
    void marksClaimedEventPublishedAfterSuccessfulDelivery() {
        TrafficOutboxEventMapper mapper = mock(TrafficOutboxEventMapper.class);
        TrafficOutboxEvent event = event(0);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(event));
        when(mapper.claim(event.getEventId())).thenReturn(1);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://notify/internal/notifications/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "service-token"))
                .andExpect(header("Idempotency-Key", event.getEventId()))
                .andRespond(withSuccess());
        TrafficOutboxDispatcher dispatcher = new TrafficOutboxDispatcher(
                mapper, new ObjectMapper(), builder, "http://notify", "service-token", 10, 3, 1, 60);

        dispatcher.dispatchPending();

        verify(mapper).markPublished(eq(event.getEventId()), any(LocalDateTime.class));
        server.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void reschedulesFailedDeliveryWithoutMarkingPublished() {
        TrafficOutboxEventMapper mapper = mock(TrafficOutboxEventMapper.class);
        TrafficOutboxEvent event = event(0);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(event));
        when(mapper.claim(event.getEventId())).thenReturn(1);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://notify/internal/notifications/events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        TrafficOutboxDispatcher dispatcher = new TrafficOutboxDispatcher(
                mapper, new ObjectMapper(), builder, "http://notify", "service-token", 10, 3, 1, 60);

        dispatcher.dispatchPending();

        verify(mapper).markFailed(eq(event.getEventId()), eq(0), eq(1), any(LocalDateTime.class), anyString());
        server.verify();
    }

    private TrafficOutboxEvent event(int retryCount) {
        TrafficOutboxEvent event = new TrafficOutboxEvent();
        event.setId(1L);
        event.setEventId("event-traffic-1");
        event.setEventType("NotificationRequested");
        event.setPayload("{\"userId\":7}");
        event.setStatus(0);
        event.setRetryCount(retryCount);
        event.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return event;
    }
}
