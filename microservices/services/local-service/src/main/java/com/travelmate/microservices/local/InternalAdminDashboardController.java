package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.AttractionOrder;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.TourOrder;
import com.travelmate.mapper.AttractionOrderMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.TourOrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/local/admin/dashboard-orders")
public class InternalAdminDashboardController {
    private final HotelOrderMapper hotelOrders;
    private final AttractionOrderMapper attractionOrders;
    private final TourOrderMapper tourOrders;
    private final String token;

    public InternalAdminDashboardController(HotelOrderMapper hotelOrders,
                                            AttractionOrderMapper attractionOrders,
                                            TourOrderMapper tourOrders,
                                            @Value("${app.internal-service-token}") String token) {
        this.hotelOrders = hotelOrders;
        this.attractionOrders = attractionOrders;
        this.tourOrders = tourOrders;
        this.token = token;
    }

    @GetMapping
    public List<Map<String, Object>> orders(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        List<Map<String, Object>> result = new ArrayList<>();
        hotelOrders.selectList(new LambdaQueryWrapper<HotelOrder>().eq(HotelOrder::getDeleted, 0))
                .forEach(order -> result.add(order("hotel", order.getAmount(), order.getStatus(),
                        order.getCreateTime(), order.getHotelName())));
        attractionOrders.selectList(new LambdaQueryWrapper<AttractionOrder>().eq(AttractionOrder::getDeleted, 0))
                .forEach(order -> result.add(order("attraction", order.getAmount(), order.getStatus(),
                        order.getCreateTime(), order.getCity())));
        tourOrders.selectList(new LambdaQueryWrapper<TourOrder>().eq(TourOrder::getDeleted, 0))
                .forEach(order -> result.add(order("tour", order.getAmount(), order.getStatus(),
                        order.getCreateTime(), order.getProductName())));
        result.sort(Comparator.comparing(InternalAdminDashboardController::createTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private Map<String, Object> order(String category, BigDecimal amount, Integer status,
                                      LocalDateTime createTime, String destination) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("category", category);
        item.put("amount", amount);
        item.put("status", status);
        item.put("createTime", createTime);
        item.put("destination", destination);
        return item;
    }

    private static LocalDateTime createTime(Map<String, Object> order) {
        Object value = order.get("createTime");
        return value instanceof LocalDateTime time ? time : null;
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        }
    }
}
