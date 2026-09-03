package com.travelmate;

import com.travelmate.dto.AiChatDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.mapper.AiChatMapper;
import com.travelmate.service.impl.AiServiceImpl;
import com.travelmate.service.impl.TravelPlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase12AiChatWorkflowTests {

    private AiServiceImpl service;
    private AiChatMapper chatMapper;

    @BeforeEach
    void setUp() {
        service = new AiServiceImpl();
        chatMapper = mock(AiChatMapper.class);
        TravelPlaceService placeService = mock(TravelPlaceService.class);
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "aiChatMapper", chatMapper);
        ReflectionTestUtils.setField(service, "travelPlaceService", placeService);
        when(chatMapper.selectList(any())).thenReturn(List.of());
        when(placeService.findExplicitTravelCity(any())).thenReturn(null);
    }

    @Test
    void intTc112StoresUserAndAssistantMessagesInSameSession() {
        AiChatDTO dto = request("session-uc12", "  帮我安排三天行程  ");

        AiChat reply = service.chat(dto, 42L);

        assertThat(reply.getUserId()).isEqualTo(42L);
        assertThat(reply.getSessionId()).isEqualTo("session-uc12");
        assertThat(reply.getRole()).isEqualTo("assistant");
        assertThat(reply.getContent()).contains("每天 3 到 5 个活动");

        ArgumentCaptor<AiChat> messages = ArgumentCaptor.forClass(AiChat.class);
        verify(chatMapper, times(2)).insert(messages.capture());
        assertThat(messages.getAllValues()).extracting(AiChat::getRole)
                .containsExactly("user", "assistant");
        assertThat(messages.getAllValues()).extracting(AiChat::getSessionId)
                .containsOnly("session-uc12");
        assertThat(messages.getAllValues().getFirst().getContent()).isEqualTo("帮我安排三天行程");
    }

    @Test
    void intTc112LoadsHistoryScopedByUserAndSessionBeforeReplying() {
        service.chat(request("thread-A", "酒店怎么选"), 77L);

        verify(chatMapper).selectList(any());
    }

    @Test
    void unitTc112GeneratesSessionWhenClientOmitsIt() {
        AiChatDTO dto = request(null, "你好");

        AiChat reply = service.chat(dto, 42L);

        assertThat(dto.getSessionId()).startsWith("session_");
        assertThat(reply.getSessionId()).isEqualTo(dto.getSessionId());
        assertThat(reply.getContent()).contains("目的地");
    }

    @Test
    void unitTc112RejectsBlankAndOversizedMessages() {
        assertThatThrownBy(() -> service.chat(request("s", "   "), 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("消息不能为空");
        assertThatThrownBy(() -> service.chat(request("s", "x".repeat(1001)), 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("1000");
    }

    private AiChatDTO request(String sessionId, String message) {
        AiChatDTO dto = new AiChatDTO();
        dto.setSessionId(sessionId);
        dto.setMessage(message);
        dto.setClientDate("2026-08-28");
        dto.setClientTimeZone("Asia/Shanghai");
        return dto;
    }
}
