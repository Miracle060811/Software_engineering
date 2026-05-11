package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {

    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Override
    public List<Hotel> searchHotels(String city, String checkIn, String checkOut,
            Integer starRating, BigDecimal minPrice, BigDecimal maxPrice) {
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();

        // 只查询上线状态的酒店
        wrapper.eq(Hotel::getStatus, 1);

        if (StringUtils.hasText(city)) {
            wrapper.eq(Hotel::getCity, city);
        }
        if (starRating != null) {
            wrapper.eq(Hotel::getStarRating, starRating);
        }
        if (minPrice != null) {
            wrapper.ge(Hotel::getAvgPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Hotel::getAvgPrice, maxPrice);
        }

        // 按评分降序排列
        wrapper.orderByDesc(Hotel::getScore);

        return list(wrapper);
    }

    @Override
    public Map<String, Object> getHotelWithRooms(Long hotelId) {
        Hotel hotel = getById(hotelId);
        if (hotel == null) {
            throw new RuntimeException("酒店不存在");
        }

        List<HotelRoom> rooms = getRoomsByHotelId(hotelId);

        Map<String, Object> result = new HashMap<>();
        result.put("hotel", hotel);
        result.put("rooms", rooms);
        return result;
    }

    @Override
    public List<HotelRoom> getRoomsByHotelId(Long hotelId) {
        LambdaQueryWrapper<HotelRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotelRoom::getHotelId, hotelId)
                .eq(HotelRoom::getStatus, 1)
                .orderByAsc(HotelRoom::getPrice);
        return hotelRoomMapper.selectList(wrapper);
    }
}
