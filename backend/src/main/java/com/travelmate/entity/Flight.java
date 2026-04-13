package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_flight")
public class Flight {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String flightNo;
    private String airline;
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal economyPrice;
    private BigDecimal businessPrice;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer status;
}
