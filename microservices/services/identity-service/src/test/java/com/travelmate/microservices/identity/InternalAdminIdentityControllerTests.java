package com.travelmate.microservices.identity;

import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminIdentityControllerTests {
    private UserMapper mapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mapper = mock(UserMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalAdminIdentityController(mapper, "shared-token")).build();
    }

    @Test
    void adminIdentityEndpointsReturnUsersAndCount() throws Exception {
        User user = new User(); user.setId(1L); user.setUsername("admin-view");
        when(mapper.selectList(any())).thenReturn(List.of(user));
        when(mapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(get("/internal/identity/admin/users").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("admin-view"));
        mockMvc.perform(get("/internal/identity/admin/count").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
    }

    @Test
    void adminCanDisableAndEnableAnotherUser() throws Exception {
        User user = new User(); user.setId(7L); user.setStatus(1);
        when(mapper.selectById(7L)).thenReturn(user);

        mockMvc.perform(post("/internal/identity/admin/users/7/disable").param("adminId", "1")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/identity/admin/users/7/enable")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());

        verify(mapper, org.mockito.Mockito.times(2)).update(org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void adminCannotDisableOwnAccount() throws Exception {
        mockMvc.perform(post("/internal/identity/admin/users/1/disable").param("adminId", "1")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminIdentityEndpointsRejectInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/identity/admin/users").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/identity/admin/count").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/identity/admin/users/7/disable").param("adminId", "1")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/identity/admin/users/7/enable")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminIdentityEndpointsRequireInternalTokenHeader() throws Exception {
        mockMvc.perform(get("/internal/identity/admin/users")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/identity/admin/count")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/identity/admin/users/not-a-number/disable").param("adminId", "1")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/identity/admin/users/7/disable")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
    }
}
