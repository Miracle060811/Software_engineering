package com.travelmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostAuditResult {
    private boolean approved;
    private String reason;
}
