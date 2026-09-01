package com.travelmate.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBootstrapToken {
    private Long id;
    private String secretFingerprint;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Integer attemptCount;
    private String lastAttemptIp;
    private String lastResult;
    private LocalDateTime createTime;
}
