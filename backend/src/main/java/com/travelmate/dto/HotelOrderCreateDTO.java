package com.travelmate.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 酒店订单创建请求DTO
 */
@Data
public class HotelOrderCreateDTO {

    /** 酒店ID */
    private Long hotelId;

    /** 房型ID */
    private Long roomId;

    /** 预订房间数 */
    private Integer roomCount;

    /** 入住日期 */
    private LocalDate checkInDate;

    /** 退房日期 */
    private LocalDate checkOutDate;

    /** 入住人姓名 */
    private String guestName;

    /** 入住人手机号 */
    private String guestPhone;

    /** 用户优惠券ID，不使用时为空 */
    private Long userCouponId;
}
