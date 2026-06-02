package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.HotelOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HotelOrderMapper extends BaseMapper<HotelOrder> {

    @Update("UPDATE tm_hotel_order SET status = 1, pay_time = NOW() WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int markPaid(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    @Update("UPDATE tm_hotel_order SET status = 4 WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int markCancelledFromPending(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    @Update("UPDATE tm_hotel_order SET status = 5 WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 1 AND deleted = 0")
    int markRefundRequested(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    @Update("UPDATE tm_hotel_order SET status = 4 WHERE order_no = #{orderNo} AND status = 5 AND deleted = 0")
    int markRefundApproved(@Param("orderNo") String orderNo);

    @Update("UPDATE tm_hotel_order SET status = 1 WHERE order_no = #{orderNo} AND status = 5 AND deleted = 0")
    int markRefundRejected(@Param("orderNo") String orderNo);
}
