package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.entity.Attraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AttractionMapper extends BaseMapper<Attraction> {

    /**
     * 乐观扣减景点门票库存 (防并发超售)
     *
     * @param attractionId 景点ID
     * @param count        购买数量
     * @return 影响行数, 0 表示票数不足
     */
    @Update("UPDATE tm_attraction SET available_tickets = available_tickets - #{count} WHERE id = #{attractionId} AND available_tickets >= #{count}")
    int deductTicket(@Param("attractionId") Long attractionId, @Param("count") Integer count);

    /**
     * 归还门票库存 (用于取消订单)
     *
     * @param attractionId 景点ID
     * @param count        归还数量
     */
    @Update("UPDATE tm_attraction SET available_tickets = available_tickets + #{count} WHERE id = #{attractionId}")
    int returnTicket(@Param("attractionId") Long attractionId, @Param("count") Integer count);
}
