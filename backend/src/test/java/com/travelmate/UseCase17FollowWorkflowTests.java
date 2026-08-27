package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.UserProfileController;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UseCase17FollowWorkflowTests {

    @Test
    void unitTc117CreatesAndCancelsUniqueFollowRelation() {
        FollowMapper followMapper = mock(FollowMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FollowServiceImpl service = service(followMapper, userMapper);
        User target = activeUser(2L, "target");
        when(userMapper.selectById(2L)).thenReturn(target);
        Follow existing = new Follow();
        existing.setId(40L);
        when(followMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);

        assertThat(service.toggleFollow(1L, 2L)).isEqualTo(1);
        verify(followMapper).insert(any(Follow.class));
        assertThat(service.toggleFollow(1L, 2L)).isZero();
        verify(followMapper).deleteById(40L);
    }

    @Test
    void unitTc117RejectsSelfAndUnavailableTargets() {
        FollowMapper followMapper = mock(FollowMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FollowServiceImpl service = service(followMapper, userMapper);

        assertThatThrownBy(() -> service.toggleFollow(1L, 1L)).hasMessage("不能关注自己");
        User disabled = activeUser(2L, "disabled");
        disabled.setStatus(0);
        when(userMapper.selectById(2L)).thenReturn(disabled);
        assertThatThrownBy(() -> service.toggleFollow(1L, 2L)).hasMessage("被关注用户不存在");
    }

    @Test
    void intTc117ProfileReturnsOnlyPublicFieldsAndCounts() {
        UserProfileController controller = new UserProfileController();
        UserMapper userMapper = mock(UserMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        FollowMapper followMapper = mock(FollowMapper.class);
        User user = activeUser(2L, "traveler");
        user.setPassword("secret-hash");
        user.setEmail("private@example.com");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(postMapper.selectCount(any(Wrapper.class))).thenReturn(3L);
        when(followMapper.selectCount(any(Wrapper.class))).thenReturn(4L, 5L);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "postMapper", postMapper);
        ReflectionTestUtils.setField(controller, "followMapper", followMapper);

        Result<Map<String, Object>> result = controller.getUserProfile("traveler");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("postCount", 3L)
                .containsEntry("followingCount", 4L)
                .containsEntry("fansCount", 5L);
        assertThat(result.getData()).doesNotContainKeys("password", "email", "phone", "token");
    }

    private FollowServiceImpl service(FollowMapper followMapper, UserMapper userMapper) {
        FollowServiceImpl service = new FollowServiceImpl();
        ReflectionTestUtils.setField(service, "followMapper", followMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        return service;
    }

    private User activeUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
