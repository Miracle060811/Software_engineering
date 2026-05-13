package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("tm_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评价用户ID */
    private Long userId;

    /** 评价目标ID (酒店ID 或 景点ID) */
    private Long targetId;

    /**
     * 目标类型:
     * 0-酒店, 1-景点
     */
    private Integer targetType;

    /** 关联的订单ID */
    private Long orderId;

    /** 评分 (1-5) */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 图片 (JSON数组字符串) */
    private String images;

    /** 评价标签（逗号分隔，如"干净卫生,性价比高"） */
    private String tags;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除: 0-正常, 1-已删除 */
    private Integer deleted;
}
