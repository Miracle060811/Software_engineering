package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_train")
public class Train {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String trainNo;
    private String trainType;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer durationMinutes;
    private BigDecimal firstClassPrice;
    private BigDecimal secondClassPrice;
    private Integer firstClassSeats;
    private Integer secondClassSeats;
    private Integer status;
}
