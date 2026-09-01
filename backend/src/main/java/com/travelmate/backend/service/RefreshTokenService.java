package com.travelmate.backend.service;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.entity.RefreshSession;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.RefreshSessionMapper;
import com.travelmate.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshSessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int refreshDays;

    public RefreshTokenService(RefreshSessionMapper sessionMapper, UserMapper userMapper, JwtUtil jwtUtil,
                               @Value("${app.security.refresh-token-days:14}") int refreshDays) {
        if (refreshDays < 1 || refreshDays > 90) {
            throw new IllegalStateException("REFRESH_TOKEN_DAYS must be between 1 and 90");
        }
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public RefreshGrant create(User user, String sourceIp, String userAgent) {
        String rawToken = newRawToken();
        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setTokenHash(hash(rawToken));
        session.setTokenVersion(tokenVersion(user));
        session.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(refreshDays));
        session.setSourceIp(limit(sourceIp, 64));
        session.setUserAgent(limit(userAgent, 255));
        sessionMapper.insert(session);
        return grant(user, rawToken);
    }

    @Transactional
    public RefreshGrant rotate(String rawToken, String sourceIp, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) return null;
        RefreshSession current = sessionMapper.selectForUpdate(hash(rawToken));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (current == null || current.getRevokedAt() != null || !now.isBefore(current.getExpiresAt())) return null;
        User user = userMapper.selectById(current.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || Integer.valueOf(1).equals(user.getDeleted())
                || tokenVersion(user) != current.getTokenVersion()) {
            sessionMapper.revoke(current.getId(), now);
            return null;
        }
        if (sessionMapper.revoke(current.getId(), now) != 1) return null;
        return create(user, sourceIp, userAgent);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        RefreshSession session = sessionMapper.selectForUpdate(hash(rawToken));
        if (session != null && session.getRevokedAt() == null) {
            sessionMapper.revoke(session.getId(), LocalDateTime.now(ZoneOffset.UTC));
        }
    }

    public int getRefreshDays() {
        return refreshDays;
    }

    private RefreshGrant grant(User user, String refreshToken) {
        String accessToken = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole(), tokenVersion(user));
        return new RefreshGrant(accessToken, refreshToken);
    }

    private String newRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法计算 refresh token 指纹", e);
        }
    }

    private int tokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private String limit(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }

    public record RefreshGrant(String accessToken, String refreshToken) {
    }
}
