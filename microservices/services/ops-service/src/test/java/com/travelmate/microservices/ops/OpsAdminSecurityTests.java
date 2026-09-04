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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminOpsController.class, AdminSecretController.class})
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
    private AdminCsvImportService csvImportService;
    @MockitoBean
    private OpsK8sSecretService k8sSecretService;
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
                new Endpoint("POST", "/api/admin/users/7/disable"),
                new Endpoint("POST", "/api/admin/users/7/enable"),
                new Endpoint("GET", "/api/admin/orders"),
                new Endpoint("GET", "/api/admin/flights"),
                new Endpoint("POST", "/api/admin/flights"),
                new Endpoint("PUT", "/api/admin/flights/1"),
                new Endpoint("DELETE", "/api/admin/flights/1"),
                new Endpoint("GET", "/api/admin/trains"),
                new Endpoint("POST", "/api/admin/trains"),
                new Endpoint("PUT", "/api/admin/trains/2"),
                new Endpoint("DELETE", "/api/admin/trains/2"),
                new Endpoint("GET", "/api/admin/hotels"),
                new Endpoint("POST", "/api/admin/hotels"),
                new Endpoint("PUT", "/api/admin/hotels/1"),
                new Endpoint("DELETE", "/api/admin/hotels/1"),
                new Endpoint("GET", "/api/admin/hotels/1/rooms"),
                new Endpoint("POST", "/api/admin/hotels/1/rooms"),
                new Endpoint("PUT", "/api/admin/hotel-rooms/2"),
                new Endpoint("DELETE", "/api/admin/hotel-rooms/2"),
                new Endpoint("GET", "/api/admin/attractions"),
                new Endpoint("POST", "/api/admin/attractions"),
                new Endpoint("PUT", "/api/admin/attractions/3"),
                new Endpoint("DELETE", "/api/admin/attractions/3"),
                new Endpoint("GET", "/api/admin/destinations"),
                new Endpoint("POST", "/api/admin/destinations/sync-home"),
                new Endpoint("DELETE", "/api/admin/destinations/7"),
                new Endpoint("GET", "/api/admin/coupons"),
                new Endpoint("POST", "/api/admin/coupons"),
                new Endpoint("PUT", "/api/admin/coupons/3"),
                new Endpoint("DELETE", "/api/admin/coupons/3"),
                new Endpoint("GET", "/api/admin/coupons/3/claims"),
                new Endpoint("POST", "/api/admin/orders/T1/refund/approve"),
                new Endpoint("POST", "/api/admin/orders/T1/refund/reject"),
                new Endpoint("POST", "/api/admin/orders/T1/ticket/complete"),
                new Endpoint("POST", "/api/admin/import/flights"),
                new Endpoint("GET", "/api/admin/posts"),
                new Endpoint("POST", "/api/admin/posts/4/approve"),
                new Endpoint("POST", "/api/admin/posts/4/reject"),
                new Endpoint("POST", "/api/admin/posts/4/metrics"),
                new Endpoint("GET", "/api/admin/review-reports"),
                new Endpoint("POST", "/api/admin/review-reports/5/resolve"),
                new Endpoint("POST", "/api/admin/review-reports/5/reject"),
                new Endpoint("POST", "/api/admin/review-reports/5/delete-review"),
                new Endpoint("GET", "/api/admin/reviews/8/replies"),
                new Endpoint("POST", "/api/admin/reviews/8/replies"),
                new Endpoint("DELETE", "/api/admin/replies/9"),
                new Endpoint("GET", "/api/admin/sensitive-words"),
                new Endpoint("POST", "/api/admin/sensitive-words"),
                new Endpoint("PUT", "/api/admin/sensitive-words/6"),
                new Endpoint("DELETE", "/api/admin/sensitive-words/6"),
                new Endpoint("GET", "/api/admin/logs")
                ,new Endpoint("GET", "/api/admin/secrets")
                ,new Endpoint("PUT", "/api/admin/secrets/deepseek")
                ,new Endpoint("PUT", "/api/admin/secrets/admin-register")
                ,new Endpoint("POST", "/api/admin/secrets/admin-register/reset")
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
            case "PUT" -> put(endpoint.path()).contentType("application/json").content("{}");
            default -> get(endpoint.path());
        };
    }

    private record Endpoint(String method, String path) {}
}
