package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_review_report")
public class ReviewReport {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewId;
    private Long reporterId;
    private String reason;
    private Integer status; // 0=待处理 1=已处理
    private LocalDateTime createTime;
}
