package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.TrafficOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrafficOrderMapper extends BaseMapper<TrafficOrder> {

    @Update("UPDATE tm_traffic_order SET status = 1, pay_time = NOW() WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int markPaid(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    @Update("UPDATE tm_traffic_order SET status = 3 WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int markCancelledFromPending(@Param("userId") Long userId, @Param("orderNo") String orderNo);
}
