package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tm_ai_plan")
public class AiPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String destination;
    private LocalDate startDate;
    private Integer days;
    private BigDecimal budget;
    private Integer peopleCount;
    private String preferences;
    private String planContent;
    private Integer status;
    private LocalDateTime createTime;
}
