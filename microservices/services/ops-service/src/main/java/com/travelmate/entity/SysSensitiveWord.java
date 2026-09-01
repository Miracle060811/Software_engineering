package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sensitive_word")
public class SysSensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private Integer level;
    private LocalDateTime createTime;
}
