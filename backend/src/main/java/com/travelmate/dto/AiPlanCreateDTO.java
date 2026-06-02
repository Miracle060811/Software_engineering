package com.travelmate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiPlanCreateDTO {
    private String destination;
    private int days;
    private BigDecimal budget;
    @JsonAlias("people")
    private int peopleCount;
    private String preferences;
    private String startDate;
    private String travelStyle;
    private String mustVisit;
    private String avoidPlaces;
    private String transportPreference;
    private String accommodationPreference;
}
