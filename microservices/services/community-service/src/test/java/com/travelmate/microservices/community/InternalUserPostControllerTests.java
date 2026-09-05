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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalUserPostControllerTests {
    private PostMapper postMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postMapper = mock(PostMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalUserPostController(postMapper, "internal-token"))
                .build();
    }

    @Test
    void returnsPublishedPostsForUser() throws Exception {
        Post post = new Post();
        post.setId(9L); post.setUserId(8L); post.setTitle("已发布游记"); post.setStatus(1);
        when(postMapper.selectList(any())).thenReturn(List.of(post));

        mockMvc.perform(get("/internal/community/users/8/posts")
                        .header("X-Internal-Token", "internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    void rejectsInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/community/users/8/posts")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMalformedUserId() throws Exception {
        mockMvc.perform(get("/internal/community/users/not-a-number/posts")
                        .header("X-Internal-Token", "internal-token"))
                .andExpect(status().isBadRequest());
    }
}
