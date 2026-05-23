package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 使用类别：all=通用, flight=机票, train=火车票, hotel=酒店 */
    private String category;

    /** 0=满减, 1=折扣 */
    private Integer discountType;

    /** 减免金额（满减时）或折扣比例如0.85（折扣时） */
    private BigDecimal discountValue;

    /** 最低消费金额 */
    private BigDecimal minAmount;

    /** 过期时间 */
    private LocalDateTime expireDate;

    /** 库存（可领取数量） */
    private Integer stock;

    /** 0=有效, 1=已过期 */
    private Integer status;

    private LocalDateTime createTime;
}
