package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tm_tour_product_step")
public class TourProductStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Integer dayNo;
    private Integer sequenceNo;
    private String placeName;
    private Long attractionId;
    private Integer stayMinutes;
    private String transportNote;
    private String sourceUrl;
}
