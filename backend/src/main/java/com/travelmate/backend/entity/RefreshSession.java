package com.travelmate.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshSession {
    private String id;
    private Long userId;
    private String tokenHash;
    private Integer tokenVersion;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String sourceIp;
    private String userAgent;
    private LocalDateTime createTime;
    private LocalDateTime lastUsedAt;
}
