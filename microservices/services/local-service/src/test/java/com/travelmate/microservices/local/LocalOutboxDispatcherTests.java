package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.mapper.LocalOutboxEventMapper;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class LocalOutboxDispatcherTests {

    @Test
    @SuppressWarnings("unchecked")
    void movesEventToDeadLetterAfterMaximumRetries() {
        LocalOutboxEventMapper mapper = mock(LocalOutboxEventMapper.class);
        LocalOutboxEvent event = new LocalOutboxEvent();
        event.setId(1L);
        event.setEventId("event-local-1");
        event.setEventType("NotificationRequested");
        event.setPayload("{\"userId\":9}");
        event.setStatus(0);
        event.setRetryCount(2);
        event.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(event));
        when(mapper.claim(event.getEventId())).thenReturn(1);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://notify/internal/notifications/events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        LocalOutboxDispatcher dispatcher = new LocalOutboxDispatcher(
                mapper, new ObjectMapper(), builder, "http://notify", "service-token", 10, 3, 1, 60);

        dispatcher.dispatchPending();

        verify(mapper).markFailed(eq(event.getEventId()), eq(2), eq(3), any(LocalDateTime.class), anyString());
        server.verify();
    }
}
