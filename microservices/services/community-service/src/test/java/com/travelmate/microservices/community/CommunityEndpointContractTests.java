package com.travelmate.microservices.community;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.controller.CommentController;
import com.travelmate.controller.LikeController;
import com.travelmate.controller.PostController;
import com.travelmate.entity.Post;
import com.travelmate.service.CommentService;
import com.travelmate.service.LikeService;
import com.travelmate.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityEndpointContractTests {
    private PostService postService;
    private LikeService likeService;
    private CommentService commentService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        likeService = mock(LikeService.class);
        commentService = mock(CommentService.class);
        userContext = mock(UserContext.class);
        PostController post = new PostController();
        ReflectionTestUtils.setField(post, "postService", postService);
        ReflectionTestUtils.setField(post, "userContext", userContext);
        LikeController like = new LikeController();
        ReflectionTestUtils.setField(like, "likeService", likeService);
        ReflectionTestUtils.setField(like, "userContext", userContext);
        CommentController comment = new CommentController();
        ReflectionTestUtils.setField(comment, "commentService", commentService);
        ReflectionTestUtils.setField(comment, "userContext", userContext);
        mockMvc = MockMvcBuilders.standaloneSetup(post, like, comment)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void communityEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        Post post = new Post(); post.setId(1L); post.setUserId(7L); post.setTitle("游记");
        when(postService.listPosts(1, 10, null)).thenReturn(List.of(Map.of("id", 1L)));
        when(postService.getPostDetail(1L, 7L)).thenReturn(post);
        when(postService.createPost(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(post);
        when(postService.updatePost(org.mockito.ArgumentMatchers.eq(1L), any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(post);
        when(postService.myPosts(7L)).thenReturn(List.of(post));
        when(postService.getFollowingPosts(7L, 1, 10, null)).thenReturn(List.of(Map.of("id", 1L)));
        when(likeService.toggleLike(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(Map.of("liked", true));
        when(likeService.likeStatus(7L, 1L, 0)).thenReturn(1);
        when(likeService.getMyCollects(7L)).thenReturn(List.of(Map.of("id", 1L)));
        when(commentService.listComments(1L)).thenReturn(List.of(Map.of("id", 2L)));
        when(commentService.addComment(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(Map.of("id", 2L));

        mockMvc.perform(get("/api/post/list")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/post/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(post("/api/post/create").contentType("application/json").content("{\"title\":\"游记\",\"content\":\"正文\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(put("/api/post/1").contentType("application/json").content("{\"title\":\"更新\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(delete("/api/post/1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/post/my")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/post/following")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(post("/api/like/toggle").contentType("application/json")
                        .content("{\"targetId\":1,\"targetType\":0,\"actionType\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.liked").value(true));
        mockMvc.perform(get("/api/like/status").param("targetId", "1").param("targetType", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/like/my/collects")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/comment/list").param("postId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(2));
        mockMvc.perform(post("/api/comment/add").contentType("application/json").content("{\"postId\":1,\"content\":\"评论\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(2));
        mockMvc.perform(delete("/api/comment/2")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void communityEndpointsEnforceAuthenticationBoundaries() throws Exception {
        when(userContext.getCurrentUserId()).thenThrow(new RuntimeException("用户未登录或Token无效"));
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);
        when(postService.listPosts(1, 10, null)).thenReturn(List.of());
        Post post = new Post(); post.setId(1L);
        when(postService.getPostDetail(1L, null)).thenReturn(post);
        when(commentService.listComments(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/post/list")).andExpect(status().isOk());
        mockMvc.perform(get("/api/post/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/comment/list").param("postId", "1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/post/create").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(put("/api/post/1").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(delete("/api/post/1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/post/my")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/post/following")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/like/toggle").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/like/status").param("targetId", "1").param("targetType", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/like/my/collects")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/comment/add").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(delete("/api/comment/2")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void communityEndpointsRejectMalformedParameters() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        mockMvc.perform(get("/api/post/list").param("page", "bad")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/post/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/post/create")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/post/not-a-number").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/post/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/post/my").param("unexpected", "ignored")).andExpect(status().isOk());
        mockMvc.perform(get("/api/post/following").param("size", "bad")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/like/toggle")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/like/status").param("targetId", "1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/like/my/collects").param("unexpected", "ignored")).andExpect(status().isOk());
        mockMvc.perform(get("/api/comment/list")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/comment/add")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/comment/not-a-number")).andExpect(status().isBadRequest());
    }

    @Test
    void opsOutageReturnsServiceUnavailableForPostMutations() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(postService.createPost(any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "内容安全服务暂不可用"));
        when(postService.updatePost(org.mockito.ArgumentMatchers.eq(1L), any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "内容安全服务暂不可用"));
        mockMvc.perform(post("/api/post/create").contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        mockMvc.perform(put("/api/post/1").contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }
}
