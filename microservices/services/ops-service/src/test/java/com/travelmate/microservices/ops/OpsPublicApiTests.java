package com.travelmate.microservices.ops;

import com.travelmate.common.UserContext;
import com.travelmate.entity.SysSensitiveWord;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpsPublicApiTests {
    @Test
    void statsAggregatesBusinessServices() throws Exception {
        OpsAggregationGateway gateway = mock(OpsAggregationGateway.class);
        OpsLocalService local = mock(OpsLocalService.class);
        UserContext userContext = mock(UserContext.class);
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(gateway.stats()).thenReturn(Map.of("totalUsers", 2, "totalOrders", 3, "pendingPosts", 1));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminOpsController(gateway, local, userContext, dashboardService, mock(AdminCsvImportService.class))).build();

        mvc.perform(get("/api/admin/stats")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(2))
                .andExpect(jsonPath("$.data.totalOrders").value(3));
    }

    @Test
    void sensitiveWordCreationNormalizesInput() throws Exception {
        OpsAggregationGateway gateway = mock(OpsAggregationGateway.class);
        OpsLocalService local = mock(OpsLocalService.class);
        UserContext userContext = mock(UserContext.class);
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(userContext.getCurrentUserId()).thenReturn(1L);
        SysSensitiveWord word = new SysSensitiveWord(); word.setId(9L); word.setWord("风险词"); word.setLevel(2);
        when(local.addSensitiveWord("  风险词  ", 2, 1L)).thenReturn(word);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminOpsController(gateway, local, userContext, dashboardService, mock(AdminCsvImportService.class))).build();

        mvc.perform(post("/api/admin/sensitive-words").contentType("application/json")
                        .content("{\"word\":\"  风险词  \",\"level\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.word").value("风险词"));
    }

    @Test
    void reviewResolutionDelegatesToLocalOwner() throws Exception {
        OpsAggregationGateway gateway = mock(OpsAggregationGateway.class);
        OpsLocalService local = mock(OpsLocalService.class);
        UserContext userContext = mock(UserContext.class);
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.resolveReport(org.mockito.ArgumentMatchers.eq(4L), any()))
                .thenReturn(Map.of("id", 4L, "status", 1));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminOpsController(gateway, local, userContext, dashboardService, mock(AdminCsvImportService.class))).build();

        mvc.perform(post("/api/admin/review-reports/4/resolve").contentType("application/json")
                        .content("{\"remark\":\"复核完成\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        verify(gateway).resolveReport(org.mockito.ArgumentMatchers.eq(4L), any());
    }

    @Test
    void logContractContainsRecordsAndRequestedSize() throws Exception {
        OpsAggregationGateway gateway = mock(OpsAggregationGateway.class);
        OpsLocalService local = mock(OpsLocalService.class);
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(local.logs(1, 20)).thenReturn(Map.of("records", List.of(), "total", 0, "page", 1, "size", 20));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new AdminOpsController(gateway, local, mock(UserContext.class), dashboardService, mock(AdminCsvImportService.class))).build();
        mvc.perform(get("/api/admin/logs?page=1&size=20")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray()).andExpect(jsonPath("$.data.size").value(20));
    }
}
