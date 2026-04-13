package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_traffic_order")
public class TrafficOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;

    /**
     * 0-机票, 1-火车票
     */
    private Integer orderType;
    private Long ticketId;

    /**
     * 舱位/席别: Economy, Business
     */
    private String seatType;

    private String passengerName;
    private String passengerIdCard;

    private BigDecimal amount;

    /**
     * 0-待支付, 1-出票中, 2-已出票, 3-已取消, 4-已退票
     */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime payTime;

    @TableLogic
    private Integer deleted;
}
