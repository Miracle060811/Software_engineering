package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 景点门票实体
 */
@Data
@TableName("tm_attraction")
public class Attraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 景点名称 */
    private String name;

    /** 所在城市 */
    private String city;

    /** 详细地址 */
    private String address;

    /** 景点描述 */
    private String description;

    /** 封面图片URL */
    private String coverImg;

    /** 成人票价 */
    private BigDecimal adultPrice;

    /** 儿童票价 */
    private BigDecimal childPrice;

    /** 总票数 */
    private Integer totalTickets;

    /** 可售票数 */
    private Integer availableTickets;

    /** 开放时间描述 (如: 08:00-18:00) */
    private String openTime;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    /** 状态: 0-下线, 1-正常 */
    private Integer status;

    /** 官方/政府来源URL */
    private String officialUrl;

    /** 数据来源名称 */
    private String sourceName;

    /** 数据核验日期 */
    private LocalDate dataCheckedDate;
}
