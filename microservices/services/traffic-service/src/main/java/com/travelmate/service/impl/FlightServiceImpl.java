package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Flight;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.service.FlightService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class FlightServiceImpl extends ServiceImpl<FlightMapper, Flight> implements FlightService {

    @Override
    public List<Flight> searchFlights(String depCity, String arrCity, String depDate) {
        LambdaQueryWrapper<Flight> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Flight::getStatus, 1);
        wrapper.gt(Flight::getAvailableSeats, 0);

        if (StringUtils.hasText(depCity)) {
            wrapper.eq(Flight::getDepartureCity, depCity);
        }
        if (StringUtils.hasText(arrCity)) {
            wrapper.eq(Flight::getArrivalCity, arrCity);
        }
        if (StringUtils.hasText(depDate)) {
            wrapper.likeRight(Flight::getDepartureTime, depDate);
        }
        wrapper.orderByAsc(Flight::getDepartureTime);

        List<Flight> results = list(wrapper);

        // 数据库里没有该日期的航班时，动态生成演示数据
        if (results.isEmpty() && StringUtils.hasText(depDate)) {
            if (StringUtils.hasText(depCity) && StringUtils.hasText(arrCity)) {
                results = generateDemoFlights(depCity, arrCity, depDate);
            } else {
                results = generateDemoFlightsMultiRoute(depDate, depCity, arrCity);
            }
        }

        return results;
    }

    private List<Flight> generateDemoFlightsMultiRoute(String depDate, String depCity, String arrCity) {
        String[][] routes = {
            {"北京", "上海"}, {"上海", "北京"}, {"北京", "广州"}, {"广州", "北京"},
            {"北京", "成都"}, {"上海", "成都"}, {"广州", "成都"}, {"北京", "三亚"},
            {"上海", "三亚"}, {"北京", "西安"}, {"上海", "杭州"}, {"广州", "重庆"}
        };
        List<Flight> all = new ArrayList<>();
        for (String[] route : routes) {
            String dep = route[0], arr = route[1];
            if (StringUtils.hasText(depCity) && !dep.equals(depCity)) continue;
            if (StringUtils.hasText(arrCity) && !arr.equals(arrCity)) continue;
            all.addAll(generateDemoFlights(dep, arr, depDate));
        }
        return all;
    }

    private List<Flight> generateDemoFlights(String depCity, String arrCity, String depDate) {
        String[][] airlines = {
            {"CA", "中国国际航空"},
            {"MU", "中国东方航空"},
            {"CZ", "中国南方航空"},
            {"HU", "海南航空"},
            {"3U", "四川航空"}
        };

        int durationMinutes = estimateDuration(depCity, arrCity);
        BigDecimal baseEconomy = BigDecimal.valueOf(Math.max(280, durationMinutes * 2L))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal baseBusiness = baseEconomy.multiply(BigDecimal.valueOf(3.2))
                .setScale(0, RoundingMode.HALF_UP);

        LocalDate date;
        try {
            date = LocalDate.parse(depDate);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        int[][] departureTimes = {{7, 30}, {10, 0}, {13, 30}, {17, 0}, {20, 30}};
        double[] priceFactors = {0.85, 0.925, 1.0, 1.075, 1.15};
        int routeSeed = Math.floorMod((depCity + arrCity).hashCode(), 100);

        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < departureTimes.length; i++) {
            String[] airline = airlines[i % airlines.length];
            LocalDateTime dep = LocalDateTime.of(date, LocalTime.of(departureTimes[i][0], departureTimes[i][1]));
            LocalDateTime arr = dep.plusMinutes(durationMinutes);

            BigDecimal ecoPrice = baseEconomy.multiply(BigDecimal.valueOf(priceFactors[i]))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal bizPrice = baseBusiness.multiply(BigDecimal.valueOf(priceFactors[i]))
                    .setScale(0, RoundingMode.HALF_UP);

            String flightNo = airline[0] + (1000 + routeSeed + i * 11);

            // 先查是否已存在（避免重复插入）
            LambdaQueryWrapper<Flight> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(Flight::getFlightNo, flightNo).likeRight(Flight::getDepartureTime, depDate);
            Flight existing = getOne(existCheck);
            if (existing != null) {
                flights.add(existing);
                continue;
            }

            Flight f = new Flight();
            f.setFlightNo(flightNo);
            f.setAirline(airline[1]);
            f.setDepartureCity(depCity);
            f.setArrivalCity(arrCity);
            f.setDepartureTime(dep);
            f.setArrivalTime(arr);
            f.setEconomyPrice(ecoPrice);
            f.setBusinessPrice(bizPrice);
            f.setTotalSeats(180);
            f.setAvailableSeats(30 + i * 15);
            f.setStatus(1);
            save(f);
            flights.add(f);
        }
        return flights;
    }

    private int estimateDuration(String depCity, String arrCity) {
        Map<String, Integer> durations = new HashMap<>();
        durations.put("北京-上海", 130);   durations.put("上海-北京", 130);
        durations.put("北京-广州", 210);   durations.put("广州-北京", 210);
        durations.put("北京-成都", 150);   durations.put("成都-北京", 150);
        durations.put("北京-三亚", 210);   durations.put("三亚-北京", 210);
        durations.put("北京-西安", 105);   durations.put("西安-北京", 105);
        durations.put("北京-杭州", 90);    durations.put("杭州-北京", 90);
        durations.put("北京-重庆", 150);   durations.put("重庆-北京", 150);
        durations.put("北京-丽江", 180);   durations.put("丽江-北京", 180);
        durations.put("上海-广州", 150);   durations.put("广州-上海", 150);
        durations.put("上海-成都", 150);   durations.put("成都-上海", 150);
        durations.put("上海-三亚", 210);   durations.put("三亚-上海", 210);
        durations.put("上海-西安", 120);   durations.put("西安-上海", 120);
        durations.put("上海-重庆", 150);   durations.put("重庆-上海", 150);
        durations.put("上海-南京", 60);    durations.put("南京-上海", 60);
        durations.put("上海-厦门", 110);   durations.put("厦门-上海", 110);
        durations.put("广州-成都", 130);   durations.put("成都-广州", 130);
        durations.put("广州-三亚", 90);    durations.put("三亚-广州", 90);
        durations.put("广州-重庆", 120);   durations.put("重庆-广州", 120);
        return durations.getOrDefault(depCity + "-" + arrCity, 120);
    }
}
