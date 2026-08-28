package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_ai_consumed_event")
public class AiConsumedEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String eventType;
    private LocalDateTime processedTime;
}
