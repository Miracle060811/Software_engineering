package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tm_tour_order")
public class TourOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Long scheduleId;
    private String productName;
    private Integer tourType;
    private LocalDate travelDate;
    private Integer participantCount;
    private String contactName;
    private String contactPhone;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String idempotencyKey;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
