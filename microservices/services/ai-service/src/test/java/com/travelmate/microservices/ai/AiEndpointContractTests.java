package com.travelmate.microservices.ai;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.entity.PrivateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiEndpointContractTests {
    private AiPlanChatService planChatService;
    private AiPrivateMessageService privateMessageService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        planChatService = mock(AiPlanChatService.class);
        privateMessageService = mock(AiPrivateMessageService.class);
        userContext = mock(UserContext.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AiPlanChatController(planChatService, userContext),
                        new AiPrivateMessageController(privateMessageService, userContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void aiPlanChatAndMessageEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        AiPlan plan = new AiPlan(); plan.setId(1L); plan.setUserId(7L); plan.setTitle("北京行程");
        AiChat chat = new AiChat(); chat.setId(2L); chat.setUserId(7L); chat.setContent("回复");
        PrivateMessage message = new PrivateMessage(); message.setId(3L); message.setSenderId(7L); message.setReceiverId(8L);
        when(planChatService.generate(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(plan);
        when(planChatService.list(7L)).thenReturn(List.of(plan));
        when(planChatService.get(1L, 7L)).thenReturn(plan);
        when(planChatService.chat(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(chat);
        when(privateMessageService.send(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(message);
        when(privateMessageService.conversation(7L, 8L)).thenReturn(List.of(message));
        when(privateMessageService.unreadCount(7L)).thenReturn(1);
        when(privateMessageService.contacts(7L)).thenReturn(List.of(Map.of("userId", 8L)));
        when(privateMessageService.searchUsers(7L, "member")).thenReturn(List.of(Map.of("userId", 8L)));

        mockMvc.perform(post("/api/ai/plan/generate").contentType("application/json")
                        .content("{\"origin\":\"上海\",\"destination\":\"北京\",\"days\":3,\"peopleCount\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(get("/api/ai/plan/list")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/ai/plan/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(post("/api/ai/chat").contentType("application/json").content("{\"message\":\"推荐酒店\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(2));
        mockMvc.perform(post("/api/private-message/send").contentType("application/json")
                        .content("{\"receiverId\":8,\"content\":\"你好\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(3));
        mockMvc.perform(get("/api/private-message/conversation/8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(3));
        mockMvc.perform(get("/api/private-message/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/private-message/contacts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].userId").value(8));
        mockMvc.perform(get("/api/private-message/users").param("keyword", "member"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].userId").value(8));
    }

    @Test
    void aiPlanChatAndMessageEndpointsRequireAuthentication() throws Exception {
        when(userContext.getCurrentUserId()).thenThrow(new RuntimeException("用户未登录或Token无效"));
        mockMvc.perform(post("/api/ai/plan/generate").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/ai/plan/list")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/ai/plan/1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/ai/chat").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/private-message/send").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/private-message/conversation/8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/private-message/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/private-message/contacts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/private-message/users").param("keyword", "member"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void aiPlanChatAndMessageEndpointsRejectMalformedInput() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(planChatService.list(7L)).thenReturn(List.of());
        when(privateMessageService.unreadCount(7L)).thenReturn(0);
        mockMvc.perform(post("/api/ai/plan/generate")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ai/plan/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ai/chat")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/private-message/send")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/private-message/conversation/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ai/plan/list").param("unexpected", "ignored")).andExpect(status().isOk());
        mockMvc.perform(get("/api/private-message/unread-count").param("unexpected", "ignored")).andExpect(status().isOk());
        mockMvc.perform(get("/api/private-message/contacts").param("unexpected", "ignored")).andExpect(status().isOk());
        mockMvc.perform(get("/api/private-message/users")).andExpect(status().isBadRequest());
    }

    @Test
    void identityOutageReturnsServiceUnavailableForPrivateMessages() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(privateMessageService.send(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用"));
        when(privateMessageService.conversation(7L, 8L))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用"));
        mockMvc.perform(post("/api/private-message/send").contentType("application/json")
                        .content("{\"receiverId\":8,\"content\":\"你好\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        mockMvc.perform(get("/api/private-message/conversation/8"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }
}
