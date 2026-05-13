package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_reply")
public class Reply {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    private Integer deleted;
}
