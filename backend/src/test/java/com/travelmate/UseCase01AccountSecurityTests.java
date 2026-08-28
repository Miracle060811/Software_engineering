package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase01AccountSecurityTests {

    private UserService service;
    private UserMapper userMapper;
    private JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new UserService();
        userMapper = mock(UserMapper.class);
        jwtUtil = mock(JwtUtil.class);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
    }

    @Test
    void unitTc101RegistersNormalizedUsernameWithBcryptHashAndUserRole() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertThat(service.register("  traveler  ", "secret123", 0)).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("traveler");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", captor.getValue().getPassword())).isTrue();
        assertThat(captor.getValue().getRole()).isZero();
    }

    @Test
    void unitTc101RejectsDuplicateAndShortPasswordWithoutInsert() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThat(service.register("traveler", "secret123", 0)).isFalse();
        assertThat(service.register("new-user", "123", 0)).isFalse();
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void unitTc101LoginRequiresMatchingPasswordAndIssuesJwt() {
        User user = user(7L, "traveler", "secret123");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(jwtUtil.generateToken(7L, "traveler", 0)).thenReturn("jwt-token");

        assertThat(service.login(" traveler ", "secret123")).isEqualTo("jwt-token");
        assertThat(service.login("traveler", "wrong-password")).isNull();
        verify(jwtUtil).generateToken(7L, "traveler", 0);
    }

    @Test
    void intTc101ChangesPasswordOnlyWhenOldPasswordMatches() {
        User user = user(7L, "traveler", "old-secret");
        when(userMapper.selectById(7L)).thenReturn(user);

        assertThat(service.changePassword(7L, "wrong", "new-secret")).isFalse();
        verify(userMapper, never()).updateById(any(User.class));

        assertThat(service.changePassword(7L, "old-secret", "new-secret")).isTrue();
        verify(userMapper).updateById(user);
        assertThat(encoder.matches("new-secret", user.getPassword())).isTrue();
    }

    @Test
    void intTc101AccountDeletionAnonymizesPersonalDataAndDisablesLogin() {
        User user = user(7L, "traveler", "secret123");
        user.setNickname("旅行者");
        user.setEmail("user@example.test");
        user.setPhone("13800138000");
        when(userMapper.selectById(7L)).thenReturn(user);

        assertThat(service.deleteAccount(7L, "secret123")).isTrue();

        assertThat(user.getUsername()).isEqualTo("deleted_7");
        assertThat(user.getNickname()).isEqualTo("已注销用户");
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPhone()).isNull();
        assertThat(user.getStatus()).isZero();
        assertThat(user.getDeleted()).isEqualTo(1);
        assertThat(encoder.matches("secret123", user.getPassword())).isFalse();
        verify(userMapper).updateById(user);
    }

    private User user(Long id, String username, String rawPassword) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(0);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
