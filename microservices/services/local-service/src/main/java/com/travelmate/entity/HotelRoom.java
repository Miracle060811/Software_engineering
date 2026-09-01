package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 酒店房型实体
 */
@Data
@TableName("tm_hotel_room")
public class HotelRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属酒店ID */
    private Long hotelId;

    /** 房型名称 (如: 豪华大床房、标准双床房) */
    private String roomType;

    /** 床型描述 */
    private String bedType;

    /** 房间面积(㎡) */
    private Integer area;

    /** 每晚价格 */
    private BigDecimal price;

    /** 总房间数 */
    private Integer totalRooms;

    /** 可用房间数 */
    private Integer availableRooms;

    /** 房间图片 (JSON数组字符串) */
    private String images;

    /** 设施列表 (JSON数组字符串) */
    private String facilities;

    /** 状态: 0-下线, 1-正常 */
    private Integer status;
}
