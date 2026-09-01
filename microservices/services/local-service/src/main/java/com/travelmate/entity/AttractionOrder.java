package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_attraction_order")
public class AttractionOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long attractionId;
    private String attractionName;
    private String city;
    private Integer adultCount;
    private Integer childCount;
    private Integer ticketCount;
    private String guestName;
    private String guestPhone;
    private BigDecimal amount;
    /** 1-已支付/待核销, 2-已核销, 4-已取消/已退款 */
    private Integer status;
    private LocalDateTime createTime;
    private Integer deleted;
}
