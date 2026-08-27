package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.AdminController;
import com.travelmate.entity.Post;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.impl.PostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase14PostWorkflowTests {

    private PostServiceImpl postService;
    private PostMapper postMapper;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl();
        postMapper = mock(PostMapper.class);
        userMapper = mock(UserMapper.class);
        FollowMapper followMapper = mock(FollowMapper.class);
        ReflectionTestUtils.setField(postService, "postMapper", postMapper);
        ReflectionTestUtils.setField(postService, "userMapper", userMapper);
        ReflectionTestUtils.setField(postService, "followMapper", followMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unitTc114CreatesPendingPostForSubmission() {
        when(postMapper.insert(any(Post.class))).thenAnswer(invocation -> {
            invocation.<Post>getArgument(0).setId(101L);
            return 1;
        });

        Post post = postService.createPost(Map.of(
                "title", "  云南七日游  ",
                "content", "行程很丰富",
                "destination", "丽江",
                "tags", "自由行",
                "visibility", "0"), 7L);

        assertThat(post.getId()).isEqualTo(101L);
        assertThat(post.getUserId()).isEqualTo(7L);
        assertThat(post.getTitle()).isEqualTo("云南七日游");
        assertThat(post.getStatus()).isZero();
        assertThat(post.getVisibility()).isZero();
        assertThat(post.getDeleted()).isZero();
        verify(postMapper).insert(post);
    }

    @Test
    void unitTc114CreatesDraftPostWithoutMandatoryContent() {
        when(postMapper.insert(any(Post.class))).thenAnswer(invocation -> {
            invocation.<Post>getArgument(0).setId(102L);
            return 1;
        });

        Post post = postService.createPost(Map.of("status", "3"), 7L);

        assertThat(post.getId()).isEqualTo(102L);
        assertThat(post.getStatus()).isEqualTo(3);
        assertThat(post.getTitle()).isEqualTo("未命名草稿");
        assertThat(post.getContent()).isEmpty();
    }

    @Test
    void unitTc114RejectsCreatingPostWithoutTitleOrContent() {
        assertThatThrownBy(() -> postService.createPost(Map.of("status", "0"), 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("标题不能为空");
    }

    @Test
    void unitTc114AuthorCanUpdatePostAndReturnsToPending() {
        Post existing = post();
        existing.setStatus(1);
        existing.setRejectReason("之前被驳回");
        when(postMapper.selectById(10L)).thenReturn(existing);
        when(userMapper.selectById(7L)).thenReturn(author());

        Post updated = postService.updatePost(10L, Map.of(
                "title", "更新后的标题",
                "content", "更新后的内容",
                "destination", "大理",
                "tags", "自驾",
                "visibility", "1"), 7L);

        assertThat(updated.getStatus()).isZero();
        assertThat(updated.getRejectReason()).isNull();
        assertThat(updated.getTitle()).isEqualTo("更新后的标题");
        assertThat(updated.getVisibility()).isEqualTo(1);
        verify(postMapper).updateById(any(Post.class));
    }

    @Test
    void unitTc114PreventsNonAuthorFromUpdatingPost() {
        Post existing = post();
        existing.setUserId(8L);
        when(postMapper.selectById(10L)).thenReturn(existing);

        assertThatThrownBy(() -> postService.updatePost(10L, Map.of(
                "title", "越权标题",
                "content", "越权内容"), 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权编辑他人游记");
        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    void unitTc114PreventsNonAuthorFromDeletingPost() {
        Post existing = post();
        existing.setUserId(8L);
        when(postMapper.selectById(10L)).thenReturn(existing);

        assertThatThrownBy(() -> postService.deletePost(10L, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权删除他人游记");
        verify(postMapper, never()).deleteById(anyLong());
    }

    @Test
    void intTc114AdminApprovesPostAndNotifiesAuthor() {
        AdminController controller = adminControllerWithPost(post());

        Result<Void> result = controller.approvePost(10L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(postMapper).update(isNull(Post.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void intTc114AdminRejectsPostWithReasonAndNotifiesAuthor() {
        AdminController controller = adminControllerWithPost(post());

        Result<Void> result = controller.rejectPost(10L, Map.of("reason", "图片违规"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(postMapper).update(isNull(Post.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void intTc114AdminRejectsPostWithDefaultReasonWhenNoneProvided() {
        AdminController controller = adminControllerWithPost(post());

        Result<Void> result = controller.rejectPost(10L, null);

        assertThat(result.getCode()).isEqualTo(200);
        verify(postMapper).update(isNull(Post.class), any(LambdaUpdateWrapper.class));
    }

    private Post post() {
        Post post = new Post();
        post.setId(10L);
        post.setUserId(7L);
        post.setTitle("云南七日游");
        post.setContent("行程很丰富");
        post.setStatus(0);
        post.setVisibility(0);
        return post;
    }

    private User author() {
        User user = new User();
        user.setId(7L);
        user.setUsername("author");
        return user;
    }

    private AdminController adminControllerWithPost(Post post) {
        AdminController controller = new AdminController();
        UserMapper userMapper = mock(UserMapper.class);
        postMapper = mock(PostMapper.class);
        NotificationCenterService notificationCenterService = mock(NotificationCenterService.class);

        User admin = new User();
        admin.setId(9L);
        admin.setUsername("admin-test");
        admin.setRole(1);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        when(postMapper.selectById(post.getId())).thenReturn(post);

        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "postMapper", postMapper);
        ReflectionTestUtils.setField(controller, "notificationCenterService", notificationCenterService);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-test", null, "ROLE_ADMIN"));
        return controller;
    }
}
