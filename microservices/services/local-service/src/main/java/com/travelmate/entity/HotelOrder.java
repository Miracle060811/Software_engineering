package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 酒店订单实体
 */
@Data
@TableName("tm_hotel_order")
public class HotelOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 酒店ID */
    private Long hotelId;

    /** 房型ID */
    private Long roomId;

    /** 酒店名称快照 */
    private String hotelName;

    /** 房型名称快照 */
    private String roomType;

    /** 预订房间数 */
    private Integer roomCount;

    /** 入住日期 */
    private LocalDate checkInDate;

    /** 退房日期 */
    private LocalDate checkOutDate;

    /** 住宿晚数 */
    private Integer nights;

    /** 入住人姓名 */
    private String guestName;

    /** 入住人手机号 */
    private String guestPhone;

    /** 订单总金额 */
    private BigDecimal amount;

    /**
     * 订单状态:
     * 0-待支付, 1-已支付, 2-已入住, 3-已完成, 4-已取消/已退款, 5-退款申请中
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 逻辑删除: 0-正常, 1-已删除 */
    private Integer deleted;
}
