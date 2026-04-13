package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Flight;

import java.util.List;

public interface FlightService extends IService<Flight> {

    /**
     * 查询航班
     * 
     * @param depCity 出发城市
     * @param arrCity 到达城市
     * @param depDate 出发日期 (格式: yyyy-MM-dd)
     */
    List<Flight> searchFlights(String depCity, String arrCity, String depDate);
}
