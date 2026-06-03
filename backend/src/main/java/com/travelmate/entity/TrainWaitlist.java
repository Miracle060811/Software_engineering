package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_train_waitlist")
public class TrainWaitlist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long trainId;
    private String trainNo;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private String seatType;
    private Integer ticketCount;
    private String passengerName;
    private String passengerIdCard;
    private Integer status;
    private LocalDateTime createTime;
}
