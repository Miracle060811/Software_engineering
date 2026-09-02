package com.travelmate.microservices.ops;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalContentSafetyControllerTests {
    @Test
    void internalTokenAndSensitiveResultAreEnforced() throws Exception {
        OpsLocalService local = mock(OpsLocalService.class);
        when(local.containsSensitiveWord("风险词内容")).thenReturn(true);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalContentSafetyController(local, "shared-token")).build();

        mvc.perform(post("/internal/ops/content/check").header("X-Internal-Token", "wrong")
                        .contentType("application/json").content("{\"content\":\"风险词内容\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/ops/content/check").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content("{\"content\":\"风险词内容\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sensitive").value(true));
    }

    @Test
    void malformedContentSafetyRequestIsRejected() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalContentSafetyController(mock(OpsLocalService.class), "shared-token")).build();
        mvc.perform(post("/internal/ops/content/check").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/internal/ops/content/check").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
