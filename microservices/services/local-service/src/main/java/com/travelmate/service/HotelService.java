package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelRoom;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 酒店服务接口 (成员B负责)
 */
public interface HotelService extends IService<Hotel> {

    /**
     * 搜索酒店
     *
     * @param city       城市
     * @param checkIn    入住日期 (yyyy-MM-dd，可选)
     * @param checkOut   退房日期 (yyyy-MM-dd，可选)
     * @param starRating 星级筛选 (可选)
     * @param minPrice   最低价格 (可选)
     * @param maxPrice   最高价格 (可选)
     * @return 酒店列表
     */
    List<Hotel> searchHotels(String city, String checkIn, String checkOut,
            Integer starRating, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取酒店详情（包含房型列表）
     *
     * @param hotelId 酒店ID
     * @return Map 包含 "hotel" 和 "rooms" 两个key
     */
    Map<String, Object> getHotelWithRooms(Long hotelId);

    /**
     * 获取酒店的所有可用房型
     *
     * @param hotelId 酒店ID
     * @return 房型列表
     */
    List<HotelRoom> getRoomsByHotelId(Long hotelId);
}
