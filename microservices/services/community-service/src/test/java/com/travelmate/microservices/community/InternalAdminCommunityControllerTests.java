package com.travelmate.microservices.community;

import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminCommunityControllerTests {
    private PostMapper mapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mapper = mock(PostMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalAdminCommunityController(mapper, "shared-token")).build();
    }

    @Test
    void adminCommunityEndpointsReturnPostsApprovalAndCount() throws Exception {
        Post post = new Post(); post.setId(1L); post.setStatus(0);
        when(mapper.selectList(any())).thenReturn(List.of(post));
        when(mapper.selectById(1L)).thenReturn(post);
        when(mapper.selectCount(any())).thenReturn(1L);
        mockMvc.perform(get("/internal/community/admin/posts").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(post("/internal/community/admin/posts/1/approve").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(1));
        mockMvc.perform(get("/internal/community/admin/posts").param("status", "2")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/community/admin/posts/1/reject")
                        .header("X-Internal-Token", "shared-token").contentType("application/json")
                        .content("{\"reason\":\"需要修改\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(2));
        mockMvc.perform(post("/internal/community/admin/posts/1/metrics")
                        .header("X-Internal-Token", "shared-token").contentType("application/json")
                        .content("{\"likeCount\":12,\"collectCount\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(12));
        mockMvc.perform(get("/internal/community/admin/pending-post-count").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
    }

    @Test
    void adminCommunityEndpointsRejectInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/community/admin/posts").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/community/admin/posts/1/approve").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/community/admin/posts/1/reject").header("X-Internal-Token", "wrong-token")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/community/admin/posts/1/metrics").header("X-Internal-Token", "wrong-token")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/community/admin/pending-post-count").header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCommunityEndpointsRejectMalformedRequests() throws Exception {
        mockMvc.perform(get("/internal/community/admin/posts")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/community/admin/posts/not-a-number/approve")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/community/admin/posts").param("status", "bad")
                        .header("X-Internal-Token", "shared-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/community/admin/pending-post-count")).andExpect(status().isBadRequest());
    }
}
