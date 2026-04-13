package com.travelmate.dto;

import lombok.Data;

@Data
public class TrainOrderCreateDTO {
    /** 选中的车票ID */
    private Long trainId;

    /** 选座类型: FirstClass (一等座) 或 SecondClass (二等座) */
    private String seatType;

    /** 选择的乘车人ID (从常用旅客管理里选出来的) */
    private Long passengerId;
}