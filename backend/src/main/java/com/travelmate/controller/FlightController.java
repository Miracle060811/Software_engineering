package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.entity.Flight;
import com.travelmate.service.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员A负责: 机票与大交通模块
 */
@RestController
@RequestMapping("/api/flight")
public class FlightController {

    @Autowired
    private FlightService flightService;

    /**
     * @param depCity 出发城市
     * @param arrCity 到达城市
     * @param date    出发日期 (如: 2024-05-01)
     * @return 航班列表
     */
    @GetMapping("/search")
    public Result<List<Flight>> search(
            @RequestParam(required = false) String depCity,
            @RequestParam(required = false) String arrCity,
            @RequestParam(required = false) String date) {

        List<Flight> list = flightService.searchFlights(depCity, arrCity, date);
        return Result.success(list);
    }

    /**
     * 获取航班详情 (用于退改签规则页面与下单预提交页面)
     */
    @GetMapping("/{id}")
    public Result<Flight> getDetail(@PathVariable Long id) {
        Flight flight = flightService.getById(id);
        if (flight == null || flight.getStatus() == 0) {
            return Result.error("航班不存在或已取消");
        }
        return Result.success(flight);
    }

}
