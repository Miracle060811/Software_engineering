package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("tm_price_history")
public class PriceHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的航班/火车ID */
    private Long ticketId;

    /** 0-机票, 1-火车票 */
    private Integer ticketType;

    /** 记录的日期 (表示预测或历史的这天的价格) */
    private LocalDate recordDate;

    /** 当日最低价格 */
    private BigDecimal lowestPrice;
}
