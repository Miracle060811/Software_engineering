package com.travelmate.dto;

import lombok.Data;

@Data
public class TrainBrowserTicket {
    private String trainNo;
    private String departureStation;
    private String arrivalStation;
    private String departureTime;
    private String arrivalTime;
    private String duration;
    private String businessSeat;
    private String firstClassSeat;
    private String secondClassSeat;
    private String hardSeat;
    private String noSeat;
}
