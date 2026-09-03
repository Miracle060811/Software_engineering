package com.travelmate;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.RefreshSession;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.RefreshSessionMapper;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTests {
    private RefreshSessionMapper sessionMapper;
    private UserMapper userMapper;
    private JwtUtil jwtUtil;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(RefreshSessionMapper.class);
        userMapper = mock(UserMapper.class);
        jwtUtil = mock(JwtUtil.class);
        service = new RefreshTokenService(sessionMapper, userMapper, jwtUtil, 14);
    }

    @Test
    void issuedRefreshTokenIsRandomButOnlyItsHashIsStored() {
        User user = user();
        when(jwtUtil.generateToken(7L, "traveler", 0, 2)).thenReturn("access-token");

        RefreshTokenService.RefreshGrant grant = service.create(user, "127.0.0.1", "test-agent");

        assertThat(grant.accessToken()).isEqualTo("access-token");
        assertThat(grant.refreshToken()).isNotBlank();
        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(grant.refreshToken());
        assertThat(captor.getValue().getTokenVersion()).isEqualTo(2);
    }

    @Test
    void refreshRotationRevokesTheOldSessionAndIssuesANewPair() {
        User user = user();
        RefreshSession current = new RefreshSession();
        current.setId("old-session");
        current.setUserId(7L);
        current.setTokenVersion(2);
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(sessionMapper.selectForUpdate(any())).thenReturn(current);
        when(sessionMapper.revoke(any(), any())).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(jwtUtil.generateToken(7L, "traveler", 0, 2)).thenReturn("new-access");

        RefreshTokenService.RefreshGrant grant = service.rotate("old-raw-token", "127.0.0.1", "test-agent");

        assertThat(grant.accessToken()).isEqualTo("new-access");
        verify(sessionMapper).revoke(any(), any());
        verify(sessionMapper).insert(any(RefreshSession.class));
    }

    @Test
    void revokedRefreshSessionCannotBeUsedAgain() {
        RefreshSession current = new RefreshSession();
        current.setRevokedAt(LocalDateTime.now());
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(sessionMapper.selectForUpdate(any())).thenReturn(current);

        assertThat(service.rotate("revoked-token", "127.0.0.1", "test-agent")).isNull();
        verify(userMapper, never()).selectById(any());
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        user.setRole(0);
        user.setStatus(1);
        user.setDeleted(0);
        user.setTokenVersion(2);
        return user;
    }
}
