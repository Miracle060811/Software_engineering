package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tm_tour_product")
public class TourProduct {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** 0=一日游, 1=周边游 */
    private Integer tourType;

    private String departureCity;

    private String destination;

    private String duration;

    private BigDecimal price;

    private String coverImg;

    /** 0=下线, 1=上线 */
    private Integer status;

    private LocalDateTime createTime;
}
