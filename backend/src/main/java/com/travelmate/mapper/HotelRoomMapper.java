package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.HotelRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HotelRoomMapper extends BaseMapper<HotelRoom> {

    /**
     * 乐观扣减房间库存 (防并发超售)
     * 利用 MySQL 行级锁与 available_rooms > 0 条件保证原子性
     *
     * @param roomId 房型ID
     * @return 影响行数, 0 表示房间不足
     */
    @Update("UPDATE tm_hotel_room SET available_rooms = available_rooms - #{count} WHERE id = #{roomId} AND status = 1 AND available_rooms >= #{count}")
    int deductRoom(@Param("roomId") Long roomId, @Param("count") Integer count);

    /**
     * 归还房间库存 (用于取消订单)
     *
     * @param roomId 房型ID
     */
    @Update("UPDATE tm_hotel_room SET available_rooms = LEAST(available_rooms + #{count}, total_rooms) WHERE id = #{roomId}")
    int returnRoom(@Param("roomId") Long roomId, @Param("count") Integer count);
}
