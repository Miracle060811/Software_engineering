package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.entity.Comment;
import com.travelmate.entity.Like;
import com.travelmate.entity.Post;
import com.travelmate.mapper.CommentMapper;
import com.travelmate.mapper.LikeMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.SensitiveWordService;
import com.travelmate.service.impl.CommentServiceImpl;
import com.travelmate.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UseCase15CommunityInteractionTests {

    @Test
    void unitTc115TogglesLikeAndUpdatesPostCount() {
        LikeMapper likeMapper = mock(LikeMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        LikeServiceImpl service = new LikeServiceImpl();
        ReflectionTestUtils.setField(service, "likeMapper", likeMapper);
        ReflectionTestUtils.setField(service, "postMapper", postMapper);
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        Post post = new Post();
        post.setId(3L);
        post.setLikeCount(11);
        when(postMapper.selectById(3L)).thenReturn(post);
        when(likeMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        Map<String, Object> result = service.toggleLike(7L, Map.of("targetId", 3L, "targetType", "post"));

        assertThat(result).containsEntry("liked", true).containsEntry("count", 11);
        verify(likeMapper).insert(any(Like.class));
        verify(postMapper).update(isNull(), any());
    }

    @Test
    void unitTc115RejectsInteractionWithMissingTarget() {
        LikeServiceImpl service = new LikeServiceImpl();
        ReflectionTestUtils.setField(service, "likeMapper", mock(LikeMapper.class));
        ReflectionTestUtils.setField(service, "postMapper", mock(PostMapper.class));
        ReflectionTestUtils.setField(service, "commentMapper", mock(CommentMapper.class));

        assertThatThrownBy(() -> service.toggleLike(7L, Map.of("targetId", 999L, "targetType", "collect")))
                .hasMessage("游记不存在");
    }

    @Test
    void intTc115CreatesCommentAndRejectsCrossPostParent() {
        CommentMapper commentMapper = mock(CommentMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        SensitiveWordService sensitive = mock(SensitiveWordService.class);
        CommentServiceImpl service = commentService(commentMapper, postMapper, sensitive);
        Post post = new Post();
        post.setId(3L);
        when(postMapper.selectById(3L)).thenReturn(post);
        when(sensitive.containsSensitiveWord(any())).thenReturn(false);
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            invocation.<Comment>getArgument(0).setId(81L);
            return 1;
        });

        Map<String, Object> created = service.addComment(
                Map.of("postId", 3L, "content", "  很实用的路线  "), 7L);
        assertThat(created).containsEntry("id", 81L).containsEntry("content", "很实用的路线");
        verify(postMapper).updateById(post);

        Comment wrongParent = new Comment();
        wrongParent.setId(90L);
        wrongParent.setPostId(4L);
        when(commentMapper.selectById(90L)).thenReturn(wrongParent);
        assertThatThrownBy(() -> service.addComment(
                Map.of("postId", 3L, "parentId", 90L, "content", "回复"), 7L))
                .hasMessage("父评论不存在或不属于当前游记");
    }

    @Test
    void unitTc115PreventsDeletingAnotherUsersComment() {
        CommentMapper mapper = mock(CommentMapper.class);
        CommentServiceImpl service = commentService(mapper, mock(PostMapper.class), mock(SensitiveWordService.class));
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setUserId(8L);
        when(mapper.selectById(10L)).thenReturn(comment);

        assertThatThrownBy(() -> service.deleteComment(10L, 7L)).hasMessage("无权删除他人评论");
        verify(mapper, never()).deleteById(10L);
    }

    private CommentServiceImpl commentService(CommentMapper commentMapper, PostMapper postMapper,
                                              SensitiveWordService sensitiveWordService) {
        CommentServiceImpl service = new CommentServiceImpl();
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "postMapper", postMapper);
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "sensitiveWordService", sensitiveWordService);
        return service;
    }
}
