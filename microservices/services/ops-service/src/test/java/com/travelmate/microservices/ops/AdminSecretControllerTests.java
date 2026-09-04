package com.travelmate.microservices.ops;

import com.travelmate.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSecretControllerTests {
    private OpsK8sSecretService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(OpsK8sSecretService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminSecretController(service, "travelmate-secrets", "travelmate-config"))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void readsAndUpdatesSecretConfiguration() throws Exception {
        when(service.getSecret("travelmate-secrets")).thenReturn(Map.of(
                "deepseek-api-key", "sk-test", "admin-register-secret", "register-test"));
        when(service.getConfigMap("travelmate-config")).thenReturn(Map.of(
                "ADMIN_REGISTER_ENABLED", "true", "ADMIN_REGISTER_EXPIRES_AT", "2026-12-31T23:59:59"));

        mockMvc.perform(get("/api/admin/secrets")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminRegisterEnabled").value(true));
        mockMvc.perform(put("/api/admin/secrets/deepseek").contentType("application/json")
                        .content("{\"apiKey\":\"sk-new\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/secrets/admin-register").contentType("application/json")
                        .content("{\"secret\":\"new-register\",\"enabled\":true,\"expiresAt\":\"2026-12-31T23:59:59\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/secrets/admin-register/reset")).andExpect(status().isOk());

        verify(service).patchSecret("travelmate-secrets", Map.of("deepseek-api-key", "sk-new"));
        verify(service).restartDeployment("ai-service");
        verify(service, org.mockito.Mockito.times(2)).restartDeployment("identity-service");
        verify(service).patchConfigMap(eq("travelmate-config"), any());
    }

    @Test
    void rejectsBlankDeepseekKey() throws Exception {
        mockMvc.perform(put("/api/admin/secrets/deepseek").contentType("application/json")
                        .content("{\"apiKey\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingSecretRequestBodies() throws Exception {
        mockMvc.perform(put("/api/admin/secrets/deepseek").contentType("application/json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/secrets/admin-register").contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
