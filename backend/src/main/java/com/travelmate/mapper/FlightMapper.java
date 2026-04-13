package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Flight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FlightMapper extends BaseMapper<Flight> {

    /**
     * 乐观扣减余票 (防止并发超卖)
     * 利用 MySQL 行锁和 available_seats > 0 的条件
     * 
     * @param id 航班ID
     * @return 影响的行数, 如果为0说明余票不足
     */
    @Update("UPDATE tm_flight SET available_seats = available_seats - 1 WHERE id = #{id} AND available_seats > 0")
    int deductSeat(@Param("id") Long id);

    /**
     * 归还余票 (用于取消订单或退票)
     * 
     * @param id 航班ID
     */
    @Update("UPDATE tm_flight SET available_seats = available_seats + 1 WHERE id = #{id}")
    int returnSeat(@Param("id") Long id);
}
