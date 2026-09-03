package com.travelmate.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourBookingCreateDTO {
    private Long productId;
    private Long scheduleId;
    private Integer participantCount;
    private String contactName;
    private String contactPhone;
    private BigDecimal expectedUnitPrice;
    private String idempotencyKey;
}
