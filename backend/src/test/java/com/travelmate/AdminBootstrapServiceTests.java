package com.travelmate;

import com.travelmate.backend.entity.AdminBootstrapToken;
import com.travelmate.backend.mapper.AdminBootstrapMapper;
import com.travelmate.backend.service.AdminBootstrapService;
import com.travelmate.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapServiceTests {
    private AdminBootstrapMapper mapper;
    private UserService userService;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AdminBootstrapMapper.class);
        userService = mock(UserService.class);
        service = new AdminBootstrapService(mapper, userService);
        ReflectionTestUtils.setField(service, "expectedSecret", "bootstrap-secret-strong");
        ReflectionTestUtils.setField(service, "expiresAtValue", Instant.now().plusSeconds(3600).toString());
    }

    @Test
    void disabledBootstrapIsRejectedAndAuditedWithoutTestingTheSecret() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThat(service.register("admin2", "secret123", "guess", "127.0.0.1"))
                .isEqualTo("管理员初始化不可用");

        verify(mapper).insertAudit(null, "admin2", "127.0.0.1", "CONFIG_DISABLED");
        verify(userService, never()).register(any(), any(), any(Integer.class));
    }

    @Test
    void wrongSecretIsRejectedAndRecordedWithoutLeakingItsValue() {
        ReflectionTestUtils.setField(service, "enabled", true);
        AdminBootstrapToken token = token(false);
        when(mapper.selectForUpdate(any())).thenReturn(token);

        assertThat(service.register("admin2", "secret123", "wrong", "10.0.0.2"))
                .isEqualTo("管理员注册密钥错误");

        verify(mapper).recordAttempt(9L, "10.0.0.2", "SECRET_MISMATCH");
        verify(mapper).insertAudit(9L, "admin2", "10.0.0.2", "SECRET_MISMATCH");
    }

    @Test
    void successfulBootstrapMarksTheTokenUsedExactlyOnce() {
        ReflectionTestUtils.setField(service, "enabled", true);
        AdminBootstrapToken token = token(false);
        when(mapper.selectForUpdate(any())).thenReturn(token);
        when(userService.hasAdministrator()).thenReturn(false);
        when(userService.register("admin2", "secret123", 1)).thenReturn(true);
        when(mapper.markUsed(eq(9L), any())).thenReturn(1);

        assertThat(service.register("admin2", "secret123", "bootstrap-secret-strong", "10.0.0.3"))
                .isNull();

        verify(mapper).markUsed(eq(9L), any());
        verify(mapper).recordAttempt(9L, "10.0.0.3", "SUCCESS");
        verify(mapper).insertAudit(9L, "admin2", "10.0.0.3", "SUCCESS");
    }

    @Test
    void usedBootstrapTokenCannotCreateAnotherAdministrator() {
        ReflectionTestUtils.setField(service, "enabled", true);
        AdminBootstrapToken token = token(true);
        when(mapper.selectForUpdate(any())).thenReturn(token);

        assertThat(service.register("admin3", "secret123", "bootstrap-secret-strong", "10.0.0.4"))
                .isEqualTo("管理员初始化密钥已失效");

        verify(userService, never()).register(any(), any(), any(Integer.class));
    }

    private AdminBootstrapToken token(boolean used) {
        AdminBootstrapToken token = new AdminBootstrapToken();
        token.setId(9L);
        if (used) token.setUsedAt(java.time.LocalDateTime.now());
        return token;
    }
}
