package com.travelmate.microservices.community;

import com.travelmate.common.UserContext;
import com.travelmate.controller.CommentController;
import com.travelmate.controller.LikeController;
import com.travelmate.controller.PostController;
import com.travelmate.entity.Post;
import com.travelmate.service.CommentService;
import com.travelmate.service.LikeService;
import com.travelmate.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommunityPublicApiTests {

    @Test
    void postListReturnsCommunityContract() throws Exception {
        PostController controller = new PostController();
        PostService service = mock(PostService.class);
        ReflectionTestUtils.setField(controller, "postService", service);
        ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
        when(service.listPosts(1, 10, null)).thenReturn(List.of(Map.of("id", 41L)));

        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/api/post/list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(41));
    }

    @Test
    void authenticatedPostCreateUsesCurrentUser() throws Exception {
        PostController controller = new PostController();
        PostService service = mock(PostService.class);
        UserContext userContext = mock(UserContext.class);
        Post created = new Post();
        created.setId(42L);
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(service.createPost(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(created);
        ReflectionTestUtils.setField(controller, "postService", service);
        ReflectionTestUtils.setField(controller, "userContext", userContext);

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/post/create").contentType("application/json")
                        .content("{\"title\":\"北京游记\",\"content\":\"正文\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(42));
    }

    @Test
    void commentAddUsesCurrentUser() throws Exception {
        CommentController controller = new CommentController();
        CommentService service = mock(CommentService.class);
        UserContext userContext = mock(UserContext.class);
        when(userContext.getCurrentUserId()).thenReturn(8L);
        when(service.addComment(any(), org.mockito.ArgumentMatchers.eq(8L)))
                .thenReturn(Map.of("id", 91L));
        ReflectionTestUtils.setField(controller, "commentService", service);
        ReflectionTestUtils.setField(controller, "userContext", userContext);

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/comment/add").contentType("application/json")
                        .content("{\"postId\":42,\"content\":\"很好\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(91));
    }

    @Test
    void likeToggleDelegatesTargetAndUser() throws Exception {
        LikeController controller = new LikeController();
        LikeService service = mock(LikeService.class);
        UserContext userContext = mock(UserContext.class);
        when(userContext.getCurrentUserId()).thenReturn(9L);
        when(service.toggleLike(org.mockito.ArgumentMatchers.eq(9L), any()))
                .thenReturn(Map.of("liked", true));
        ReflectionTestUtils.setField(controller, "likeService", service);
        ReflectionTestUtils.setField(controller, "userContext", userContext);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/like/toggle").contentType("application/json")
                        .content("{\"targetId\":42,\"targetType\":0,\"actionType\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.liked").value(true));
        verify(service).toggleLike(org.mockito.ArgumentMatchers.eq(9L), any());
    }
}
