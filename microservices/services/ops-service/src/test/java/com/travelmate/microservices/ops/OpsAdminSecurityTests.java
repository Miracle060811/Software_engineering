package com.travelmate.microservices.ops;

import com.travelmate.backend.config.JwtFilter;
import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.config.SecurityConfig;
import com.travelmate.common.AuthenticatedUser;
import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.mapper.SysLogMapper;
import com.travelmate.mapper.SysSensitiveWordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOpsController.class)
@Import({SecurityConfig.class, JwtFilter.class, GlobalExceptionHandler.class})
class OpsAdminSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpsAggregationGateway gateway;
    @MockitoBean
    private OpsLocalService localService;
    @MockitoBean
    private UserContext userContext;
    @MockitoBean
    private AdminDashboardService dashboardService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private SysLogMapper sysLogMapper;
    @MockitoBean
    private SysSensitiveWordMapper sysSensitiveWordMapper;

    @BeforeEach
    void setUpTokens() {
        when(jwtUtil.extractPrincipal("user-token"))
                .thenReturn(new AuthenticatedUser(7L, "member", 0));
    }

    @Test
    void allAdminEndpointsRejectAnonymousAndNonAdmin() throws Exception {
        List<Endpoint> endpoints = List.of(
                new Endpoint("GET", "/api/admin/stats"),
                new Endpoint("GET", "/api/admin/dashboard/data"),
                new Endpoint("GET", "/api/admin/users"),
                new Endpoint("GET", "/api/admin/orders"),
                new Endpoint("GET", "/api/admin/flights"),
                new Endpoint("GET", "/api/admin/posts"),
                new Endpoint("POST", "/api/admin/posts/4/approve"),
                new Endpoint("GET", "/api/admin/review-reports"),
                new Endpoint("POST", "/api/admin/review-reports/5/resolve"),
                new Endpoint("GET", "/api/admin/sensitive-words"),
                new Endpoint("POST", "/api/admin/sensitive-words"),
                new Endpoint("DELETE", "/api/admin/sensitive-words/6"),
                new Endpoint("GET", "/api/admin/logs")
        );

        for (Endpoint endpoint : endpoints) {
            mockMvc.perform(request(endpoint).with(csrf())).andExpect(status().isForbidden());
            mockMvc.perform(request(endpoint).with(csrf()).header("Authorization", "Bearer user-token"))
                    .andExpect(status().isForbidden());
        }
    }

    private MockHttpServletRequestBuilder request(Endpoint endpoint) {
        return switch (endpoint.method()) {
            case "POST" -> post(endpoint.path()).contentType("application/json").content("{}");
            case "DELETE" -> delete(endpoint.path());
            default -> get(endpoint.path());
        };
    }

    private record Endpoint(String method, String path) {}
}
