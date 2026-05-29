package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tm_destination")
public class Destination {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String name;
    private String country;
    private String tag;
    private String keywords;
    private String img;
    @TableField("`desc`")
    private String desc;
    private String intro;
    private String highlights;
    private String culture;
    private String bestSeason;
    private String transport;
    private String sourceName;
    private String sourceUrl;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
