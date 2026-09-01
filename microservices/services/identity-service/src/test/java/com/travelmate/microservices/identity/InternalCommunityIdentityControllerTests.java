package com.travelmate.microservices.identity;

import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalCommunityIdentityControllerTests {
    private UserMapper userMapper;
    private FollowMapper followMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        followMapper = mock(FollowMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalCommunityIdentityController(userMapper, followMapper, "shared-token")).build();
    }

    @Test
    void communityIdentityEndpointsReturnOwnedData() throws Exception {
        User user = new User(); user.setId(1L); user.setUsername("member");
        Follow follow = new Follow(); follow.setFolloweeId(2L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(followMapper.selectCount(any())).thenReturn(1L);
        when(followMapper.selectList(any())).thenReturn(List.of(follow));

        mockMvc.perform(get("/internal/identity/community/users/1").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("member"));
        mockMvc.perform(get("/internal/identity/community/users").param("ids", "1")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(get("/internal/identity/community/follows/status")
                        .param("followerId", "1").param("followeeId", "2")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
        mockMvc.perform(get("/internal/identity/community/following/1")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(2));
    }

    @Test
    void communityIdentityEndpointsRejectInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/identity/community/users/1").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/identity/community/users").param("ids", "1")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/identity/community/follows/status")
                        .param("followerId", "1").param("followeeId", "2")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/identity/community/following/1")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void communityIdentityEndpointsRejectMalformedParameters() throws Exception {
        mockMvc.perform(get("/internal/identity/community/users/not-a-number")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/identity/community/users").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/identity/community/follows/status")
                        .param("followerId", "1").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/identity/community/following/not-a-number")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
    }
}
