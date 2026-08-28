package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.microservices.ai.AiConsumedEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiConsumedEventMapper extends BaseMapper<AiConsumedEvent> {
    @Insert("""
            INSERT IGNORE INTO tm_ai_consumed_event (event_id, event_type, processed_time)
            VALUES (#{eventId}, #{eventType}, NOW())
            """)
    int insertIfAbsent(@Param("eventId") String eventId, @Param("eventType") String eventType);
}
