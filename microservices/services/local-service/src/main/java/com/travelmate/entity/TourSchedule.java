package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tm_tour_schedule")
public class TourSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private LocalDate travelDate;
    private BigDecimal unitPrice;
    private Integer totalStock;
    private Integer availableStock;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
