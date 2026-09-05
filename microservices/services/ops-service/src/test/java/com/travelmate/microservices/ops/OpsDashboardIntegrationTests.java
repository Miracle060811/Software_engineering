package com.travelmate.microservices.ops;

import com.travelmate.backend.config.JwtFilter;
import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.config.SecurityConfig;
import com.travelmate.common.AuthenticatedUser;
import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOpsController.class)
@Import({SecurityConfig.class, JwtFilter.class, GlobalExceptionHandler.class, AdminDashboardService.class})
class OpsDashboardIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpsAggregationGateway gateway;
    @MockitoBean
    private OpsLocalService localService;
    @MockitoBean
    private AdminCsvImportService csvImportService;
    @MockitoBean
    private UserContext userContext;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private SysLogMapper sysLogMapper;
    @MockitoBean
    private SysSensitiveWordMapper sysSensitiveWordMapper;

    @Test
    void dashboardEnforcesJwtRoleAndKeepsHttp200ForPartialDownstreamData() throws Exception {
        when(jwtUtil.extractPrincipal("member-token"))
                .thenReturn(new AuthenticatedUser(7L, "member", 0));
        when(jwtUtil.extractPrincipal("admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", 1));

        mockMvc.perform(get("/api/admin/dashboard/data"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard/data")
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isForbidden());

        when(gateway.users()).thenReturn(List.of(Map.of("id", 7L, "createTime", "2026-09-04T08:00:00")));
        when(gateway.orders()).thenThrow(new RuntimeException("traffic-service unavailable"));
        when(gateway.localOrders()).thenReturn(List.of());
        when(gateway.pendingPostCount()).thenReturn(2L);
        when(localService.dashboardMetrics()).thenReturn(Map.of(
                "onlineUsers", 1L,
                "qpsTrend", List.of(),
                "latencyTrend", List.of(),
                "recentErrors", List.of(),
                "errorLogsToday", 0L));

        mockMvc.perform(get("/api/admin/dashboard/data")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.partial").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1))
                .andExpect(jsonPath("$.data.totalOrders").doesNotExist())
                .andExpect(jsonPath("$.data.pendingPosts").value(2))
                .andExpect(jsonPath("$.data.sourceStatus.traffic-service").value("unavailable"))
                .andExpect(jsonPath("$.data.sourceStatus.identity-service").value("available"))
                .andExpect(jsonPath("$.data.degradedSources[?(@.source == 'traffic-service')]").isNotEmpty())
                .andExpect(jsonPath("$.data.alerts").isArray());
    }

    @Test
    void dashboardCombinesTrafficAndLocalOrdersWithoutAFalseDegradation() throws Exception {
        when(jwtUtil.extractPrincipal("admin-token"))
                .thenReturn(new AuthenticatedUser(1L, "admin", 1));
        when(gateway.users()).thenReturn(List.of());
        when(gateway.orders()).thenReturn(List.of(Map.of(
                "orderType", 0, "status", 2, "amount", 500,
                "createTime", "2026-09-05T08:00:00", "arrivalCity", "上海")));
        when(gateway.localOrders()).thenReturn(List.of(Map.of(
                "category", "hotel", "status", 3, "amount", 800,
                "createTime", "2026-09-05T09:00:00", "destination", "杭州")));
        when(gateway.pendingPostCount()).thenReturn(0L);
        when(localService.dashboardMetrics()).thenReturn(Map.of(
                "onlineUsers", 1L, "qpsTrend", List.of(), "latencyTrend", List.of(),
                "recentErrors", List.of(), "errorLogsToday", 0L));

        mockMvc.perform(get("/api/admin/dashboard/data")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partial").value(false))
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.todayOrders").value(2))
                .andExpect(jsonPath("$.data.todayGmv").value(1300))
                .andExpect(jsonPath("$.data.sourceStatus.local-service").value("available"))
                .andExpect(jsonPath("$.data.degradedSources").isEmpty())
                .andExpect(jsonPath("$.data.orderTypeDist[?(@.name == '酒店')].value").value(1));
    }
}
