package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 酒店信息实体 (成员B负责: 酒店住宿与本地生活子系统)
 */
@Data
@TableName("tm_hotel")
public class Hotel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 酒店名称 */
    private String name;

    /** 所在城市 */
    private String city;

    /** 详细地址 */
    private String address;

    /** 星级 (1-5) */
    private Integer starRating;

    /** 酒店描述 */
    private String description;

    /** 封面图片URL */
    private String coverImg;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    /** 均价 */
    private BigDecimal avgPrice;

    /** 评分 (0.0-5.0) */
    private BigDecimal score;

    /** 状态: 0-下线, 1-正常 */
    private Integer status;
}
