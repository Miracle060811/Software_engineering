package com.travelmate.microservices.ops;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.entity.SysSensitiveWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpsEndpointContractTests {
    private OpsAggregationGateway gateway;
    private OpsLocalService localService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gateway = mock(OpsAggregationGateway.class);
        localService = mock(OpsLocalService.class);
        userContext = mock(UserContext.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOpsController(gateway, localService, userContext))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void adminOpsEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.stats()).thenReturn(Map.of("totalUsers", 2L, "totalOrders", 3L));
        when(gateway.users()).thenReturn(List.of(Map.of("id", 1L)));
        when(gateway.orders()).thenReturn(List.of(Map.of("id", 2L)));
        when(gateway.flights()).thenReturn(List.of(Map.of("id", 3L)));
        when(gateway.posts()).thenReturn(List.of(Map.of("id", 4L)));
        when(gateway.approvePost(4L)).thenReturn(Map.of("id", 4L, "status", 1));
        when(gateway.reviewReports(null)).thenReturn(List.of(Map.of("id", 5L)));
        when(gateway.resolveReport(org.mockito.ArgumentMatchers.eq(5L), any())).thenReturn(Map.of("id", 5L, "status", 1));
        SysSensitiveWord word = new SysSensitiveWord(); word.setId(6L); word.setWord("风险词");
        when(localService.listSensitiveWords()).thenReturn(List.of(word));
        when(localService.addSensitiveWord("风险词", 2, 1L)).thenReturn(word);
        when(localService.logs(1, 20)).thenReturn(Map.of("records", List.of(), "total", 0, "page", 1, "size", 20));

        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalUsers").value(2));
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(2));
        mockMvc.perform(get("/api/admin/flights")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(3));
        mockMvc.perform(get("/api/admin/posts")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(4));
        mockMvc.perform(post("/api/admin/posts/4/approve")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mockMvc.perform(get("/api/admin/review-reports")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(5));
        mockMvc.perform(post("/api/admin/review-reports/5/resolve").contentType("application/json")
                        .content("{\"remark\":\"已处理\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mockMvc.perform(get("/api/admin/sensitive-words")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(6));
        mockMvc.perform(post("/api/admin/sensitive-words").contentType("application/json")
                        .content("{\"word\":\"风险词\",\"level\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.word").value("风险词"));
        mockMvc.perform(delete("/api/admin/sensitive-words/6")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/admin/logs")).andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void adminOpsEndpointsRejectMalformedParameters() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.stats()).thenReturn(Map.of());
        when(gateway.users()).thenReturn(List.of());
        when(gateway.orders()).thenReturn(List.of());
        when(gateway.flights()).thenReturn(List.of());
        when(gateway.posts()).thenReturn(List.of());
        when(localService.listSensitiveWords()).thenReturn(List.of());
        for (String path : List.of("/api/admin/stats", "/api/admin/users", "/api/admin/orders", "/api/admin/flights",
                "/api/admin/posts", "/api/admin/sensitive-words")) {
            mockMvc.perform(get(path).param("unexpected", "ignored")).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/admin/posts/not-a-number/approve")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/review-reports").param("status", "bad")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/review-reports/not-a-number/resolve")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/sensitive-words")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/admin/sensitive-words/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/logs").param("page", "bad")).andExpect(status().isBadRequest());
    }

    @Test
    void downstreamOutageReturnsServiceUnavailableForAdminAggregation() throws Exception {
        ResponseStatusException unavailable = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "业务服务暂不可用，请稍后重试");
        when(gateway.stats()).thenThrow(unavailable);
        when(gateway.users()).thenThrow(unavailable);
        when(gateway.orders()).thenThrow(unavailable);
        when(gateway.flights()).thenThrow(unavailable);
        when(gateway.posts()).thenThrow(unavailable);
        when(gateway.approvePost(4L)).thenThrow(unavailable);
        when(gateway.reviewReports(null)).thenThrow(unavailable);
        when(gateway.resolveReport(org.mockito.ArgumentMatchers.eq(5L), any())).thenThrow(unavailable);
        for (String path : List.of("/api/admin/stats", "/api/admin/users", "/api/admin/orders", "/api/admin/flights",
                "/api/admin/posts", "/api/admin/review-reports")) {
            mockMvc.perform(get(path)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        }
        mockMvc.perform(post("/api/admin/posts/4/approve"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        mockMvc.perform(post("/api/admin/review-reports/5/resolve").contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }
}
