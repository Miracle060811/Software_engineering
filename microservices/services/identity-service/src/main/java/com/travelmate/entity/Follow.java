package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_follow")
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long followerId;
    private Long followeeId;
    private LocalDateTime createTime;
}
