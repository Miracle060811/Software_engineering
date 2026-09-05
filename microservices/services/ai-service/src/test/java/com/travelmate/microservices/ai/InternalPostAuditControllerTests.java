package com.travelmate.microservices.ai;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalPostAuditControllerTests {
    @Test
    void internalPostAuditReturnsDecisionAndEnforcesToken() throws Exception {
        AiPostAuditService service = mock(AiPostAuditService.class);
        when(service.audit(any())).thenReturn(new AiPostAuditService.AuditDecision(true, null));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalPostAuditController(service, "shared-token")).build();
        mvc.perform(post("/internal/ai/post-audit").header("X-Internal-Token", "wrong")
                        .contentType("application/json").content("{\"title\":\"游记\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/ai/post-audit").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content("{\"title\":\"游记\",\"content\":\"正文\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void malformedInternalPostAuditRequestIsRejected() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalPostAuditController(mock(AiPostAuditService.class), "shared-token")).build();
        mvc.perform(post("/internal/ai/post-audit").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
    }
}
