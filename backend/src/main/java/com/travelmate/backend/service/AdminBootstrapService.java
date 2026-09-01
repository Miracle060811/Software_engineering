package com.travelmate.backend.service;

import com.travelmate.backend.entity.AdminBootstrapToken;
import com.travelmate.backend.mapper.AdminBootstrapMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;

@Service
public class AdminBootstrapService {
    private final AdminBootstrapMapper bootstrapMapper;
    private final UserService userService;

    @Value("${app.security.admin-register-enabled:false}")
    private boolean enabled;
    @Value("${app.security.admin-register-secret:}")
    private String expectedSecret;
    @Value("${app.security.admin-register-expires-at:}")
    private String expiresAtValue;

    public AdminBootstrapService(AdminBootstrapMapper bootstrapMapper, UserService userService) {
        this.bootstrapMapper = bootstrapMapper;
        this.userService = userService;
    }

    @Transactional
    public String register(String username, String password, String suppliedSecret, String sourceIp) {
        String safeUsername = username == null ? null : username.trim();
        String safeIp = StringUtils.hasText(sourceIp) ? sourceIp : "unknown";
        Instant expiresAt = parseExpiration();
        if (!enabled || !StringUtils.hasText(expectedSecret) || expiresAt == null) {
            audit(null, safeUsername, safeIp, "CONFIG_DISABLED");
            return "管理员初始化不可用";
        }

        String fingerprint = fingerprint(expectedSecret);
        bootstrapMapper.createTokenIfAbsent(fingerprint, LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
        AdminBootstrapToken token = bootstrapMapper.selectForUpdate(fingerprint);
        if (token == null) {
            audit(null, safeUsername, safeIp, "TOKEN_STATE_MISSING");
            return "管理员初始化不可用";
        }
        if (token.getUsedAt() != null) {
            return reject(token, safeUsername, safeIp, "TOKEN_USED", "管理员初始化密钥已失效");
        }
        if (!Instant.now().isBefore(expiresAt)) {
            return reject(token, safeUsername, safeIp, "TOKEN_EXPIRED", "管理员初始化密钥已过期");
        }
        if (!secureEquals(expectedSecret, suppliedSecret)) {
            return reject(token, safeUsername, safeIp, "SECRET_MISMATCH", "管理员注册密钥错误");
        }
        if (userService.hasAdministrator()) {
            return reject(token, safeUsername, safeIp, "ADMIN_ALREADY_EXISTS", "管理员初始化不可用");
        }
        if (!userService.register(safeUsername, password, 1)) {
            return reject(token, safeUsername, safeIp, "ACCOUNT_REJECTED", "用户名已存在或密码不合法");
        }

        if (bootstrapMapper.markUsed(token.getId(), LocalDateTime.now(ZoneOffset.UTC)) != 1) {
            throw new IllegalStateException("管理员初始化密钥状态更新失败");
        }
        bootstrapMapper.recordAttempt(token.getId(), safeIp, "SUCCESS");
        audit(token.getId(), safeUsername, safeIp, "SUCCESS");
        return null;
    }

    private String reject(AdminBootstrapToken token, String username, String sourceIp,
                          String result, String message) {
        bootstrapMapper.recordAttempt(token.getId(), sourceIp, result);
        audit(token.getId(), username, sourceIp, result);
        return message;
    }

    private void audit(Long tokenId, String username, String sourceIp, String result) {
        bootstrapMapper.insertAudit(tokenId, username, sourceIp, result);
    }

    private Instant parseExpiration() {
        if (!StringUtils.hasText(expiresAtValue)) return null;
        try {
            return Instant.parse(expiresAtValue.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("无法计算管理员初始化密钥指纹", e);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
