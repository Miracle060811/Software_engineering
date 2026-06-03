package com.travelmate.dto;

import lombok.Data;

@Data
public class TrainWaitlistCreateDTO {
    private Long trainId;
    private String trainNo;
    private String departureStation;
    private String arrivalStation;
    private String departureTime;
    private String seatType;
    private Integer ticketCount;
    private Long passengerId;
}
