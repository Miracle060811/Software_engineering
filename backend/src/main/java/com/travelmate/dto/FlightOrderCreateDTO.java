package com.travelmate.dto;

import lombok.Data;

@Data
public class FlightOrderCreateDTO {
    /** 选中的航班ID */
    private Long flightId;

    /** 舱位类型: Economy (经济舱) 或 Business (商务舱) */
    private String seatType;

    /** 选择的乘车人ID (从常用旅客管理里选出来的) */
    private Long passengerId;
}
