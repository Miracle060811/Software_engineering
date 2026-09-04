package com.travelmate.microservices.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.AiPlan;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.mapper.AiPlanMapper;
import com.travelmate.mapper.PrivateContactMapper;
import com.travelmate.mapper.PrivateMessageMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBusinessSliceTests {
    @Test
    void generatedPlanIsPersistedWithDeterministicTwoDayContent() {
        AiPlanMapper planMapper = mock(AiPlanMapper.class);
        doAnswer(invocation -> { ((AiPlan) invocation.getArgument(0)).setId(31L); return 1; })
                .when(planMapper).insert(any(AiPlan.class));
        AiPlanChatService service = new AiPlanChatService(planMapper, mock(AiChatMapper.class), new ObjectMapper());
        AiPlanCreateDTO dto = new AiPlanCreateDTO();
        dto.setOrigin("上海"); dto.setDestination("杭州"); dto.setDays(2);
        dto.setBudget(new BigDecimal("3000")); dto.setPeopleCount(2);
        dto.setStartDate(LocalDate.now().plusDays(7).toString());

        AiItineraryService itinerary = new AiItineraryService();
        org.springframework.test.util.ReflectionTestUtils.setField(itinerary, "aiPlanMapper", planMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(itinerary, "notificationMapper", mock(com.travelmate.mapper.NotificationMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(itinerary, "travelPlaceService", new TravelPlaceService());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "itineraryService", itinerary);
        org.springframework.test.util.ReflectionTestUtils.setField(itinerary, "travelContextGateway", mock(AiTravelContextGateway.class));
        AiPlan plan = service.generate(dto, 7L);
        assertEquals(31L, plan.getId());
        assertEquals(7L, plan.getUserId());
        assertEquals(2, plan.getDays());
        assertTrue(plan.getPlanContent().contains("\"locationVerified\":true"));
        assertTrue(plan.getPlanContent().contains("\"day\":2"));
    }

    @Test
    void blankChatMessageIsRejected() {
        AiPlanChatService service = new AiPlanChatService(
                mock(AiPlanMapper.class), mock(AiChatMapper.class), new ObjectMapper());
        AiChatDTO dto = new AiChatDTO(); dto.setMessage("   ");
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.chat(dto, 7L));
        assertTrue(error.getMessage().contains("消息不能为空"));
    }

    @Test
    void hotelQuestionKeepsTheRequestedTopicInOfflineReply() {
        AiChatMapper chatMapper = mock(AiChatMapper.class);
        when(chatMapper.selectList(any())).thenReturn(java.util.List.of());
        AiPlanChatService service = new AiPlanChatService(
                mock(AiPlanMapper.class), chatMapper, new ObjectMapper());
        AiChatDTO dto = new AiChatDTO();
        dto.setSessionId("hotel-topic");
        dto.setMessage("酒店怎么选");

        assertTrue(service.chat(dto, 7L).getContent().contains("酒店"));
    }

    @Test
    void outsiderCannotReadAnotherUsersPlan() {
        AiPlanMapper mapper = mock(AiPlanMapper.class);
        AiPlan plan = new AiPlan(); plan.setId(4L); plan.setUserId(8L);
        when(mapper.selectById(4L)).thenReturn(plan);
        AiPlanChatService service = new AiPlanChatService(mapper, mock(AiChatMapper.class), new ObjectMapper());
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.get(4L, 9L));
        assertTrue(error.getMessage().contains("无权访问"));
    }

    @Test
    void privateMessageTrimsContentAndCreatesUnreadContact() {
        PrivateMessageMapper messageMapper = mock(PrivateMessageMapper.class);
        PrivateContactMapper contactMapper = mock(PrivateContactMapper.class);
        AiIdentityGateway identity = mock(AiIdentityGateway.class);
        when(identity.isAvailable(8L)).thenReturn(true);
        when(contactMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> { invocation.<com.travelmate.entity.PrivateMessage>getArgument(0).setId(88L); return 1; })
                .when(messageMapper).insert(any(com.travelmate.entity.PrivateMessage.class));
        AiPrivateMessageService service = new AiPrivateMessageService(messageMapper, contactMapper, identity);
        PrivateMessageSendDTO dto = new PrivateMessageSendDTO(); dto.setReceiverId(8L); dto.setContent("  你好  ");

        var message = service.send(7L, dto);
        assertEquals(88L, message.getId());
        assertEquals("你好", message.getContent());
    }
}
