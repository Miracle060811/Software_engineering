package com.travelmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmate.microservices.traffic.TrafficOutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface TrafficOutboxEventMapper extends BaseMapper<TrafficOutboxEvent> {
    @Update("""
            UPDATE tm_traffic_outbox_event
            SET status = 3, update_time = NOW()
            WHERE event_id = #{eventId} AND status = 0 AND next_retry_time <= NOW()
            """)
    int claim(@Param("eventId") String eventId);

    @Update("""
            UPDATE tm_traffic_outbox_event
            SET status = 1, published_time = #{publishedTime}, last_error = NULL, update_time = NOW()
            WHERE event_id = #{eventId} AND status = 3
            """)
    int markPublished(@Param("eventId") String eventId, @Param("publishedTime") LocalDateTime publishedTime);

    @Update("""
            UPDATE tm_traffic_outbox_event
            SET status = #{status}, retry_count = #{retryCount}, next_retry_time = #{nextRetryTime},
                last_error = #{lastError}, update_time = NOW()
            WHERE event_id = #{eventId} AND status = 3
            """)
    int markFailed(@Param("eventId") String eventId,
                   @Param("status") int status,
                   @Param("retryCount") int retryCount,
                   @Param("nextRetryTime") LocalDateTime nextRetryTime,
                   @Param("lastError") String lastError);

    @Update("""
            UPDATE tm_traffic_outbox_event
            SET status = 0, next_retry_time = NOW(), last_error = 'stale claim recovered', update_time = NOW()
            WHERE status = 3 AND update_time < #{cutoff}
            """)
    int releaseStaleClaims(@Param("cutoff") LocalDateTime cutoff);
}
