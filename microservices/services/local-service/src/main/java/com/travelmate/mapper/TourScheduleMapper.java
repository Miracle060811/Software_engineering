package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.TourSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TourScheduleMapper extends BaseMapper<TourSchedule> {

    @Update("UPDATE tm_tour_schedule "
            + "SET available_stock = available_stock - #{count}, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{scheduleId} AND product_id = #{productId} AND status = 1 "
            + "AND travel_date >= CURRENT_DATE AND available_stock >= #{count}")
    int deductStock(@Param("scheduleId") Long scheduleId,
            @Param("productId") Long productId,
            @Param("count") Integer count);
}
