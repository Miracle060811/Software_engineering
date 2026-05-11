package com.travelmate.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiPlanCreateDTO {
    private String destination;
    private int days;
    private BigDecimal budget;
    private int peopleCount;
    private String preferences;
    private String startDate;
}
