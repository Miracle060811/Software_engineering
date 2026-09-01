package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_private_contact")
public class PrivateContact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long contactUserId;
    private Long lastMessageId;
    private Integer unreadCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
