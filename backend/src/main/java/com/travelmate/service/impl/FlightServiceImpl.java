package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Flight;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.service.FlightService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FlightServiceImpl extends ServiceImpl<FlightMapper, Flight> implements FlightService {

    @Override
    public List<Flight> searchFlights(String depCity, String arrCity, String depDate) {
        LambdaQueryWrapper<Flight> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Flight::getStatus, 1);
        wrapper.gt(Flight::getAvailableSeats, 0); // 必须有余票

        if (StringUtils.hasText(depCity)) {
            wrapper.eq(Flight::getDepartureCity, depCity);
        }
        if (StringUtils.hasText(arrCity)) {
            wrapper.eq(Flight::getArrivalCity, arrCity);
        }
        if (StringUtils.hasText(depDate)) {
            // 简单的按日期前缀匹配或区间检索
            wrapper.likeRight(Flight::getDepartureTime, depDate);
        }

        // 按照出发时间升序
        wrapper.orderByAsc(Flight::getDepartureTime);

        return list(wrapper);
    }
}
