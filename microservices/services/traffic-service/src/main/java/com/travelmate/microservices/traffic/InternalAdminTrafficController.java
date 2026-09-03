package com.travelmate.microservices.traffic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Train;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/internal/traffic/admin")
public class InternalAdminTrafficController {
    private final FlightMapper flightMapper;
    private final TrainMapper trainMapper;
    private final TrafficOrderMapper orderMapper;
    private final String token;

    public InternalAdminTrafficController(FlightMapper flightMapper, TrainMapper trainMapper, TrafficOrderMapper orderMapper,
                                          @Value("${app.internal-service-token}") String token) {
        this.flightMapper = flightMapper;
        this.trainMapper = trainMapper;
        this.orderMapper = orderMapper;
        this.token = token;
    }

    @GetMapping("/flights")
    public List<Flight> flights(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return flightMapper.selectList(new LambdaQueryWrapper<Flight>().orderByDesc(Flight::getDepartureTime));
    }

    @GetMapping("/orders")
    public List<TrafficOrder> orders(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        List<TrafficOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<TrafficOrder>().orderByDesc(TrafficOrder::getCreateTime));
        orders.forEach(this::attachRouteSnapshot);
        return orders;
    }

    @GetMapping("/order-count")
    public long orderCount(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return orderMapper.selectCount(null);
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }

    private void attachRouteSnapshot(TrafficOrder order) {
        if (order.getTicketId() == null) return;
        if (Integer.valueOf(0).equals(order.getOrderType())) {
            Flight flight = flightMapper.selectById(order.getTicketId());
            if (flight != null) {
                order.setDepartureCity(flight.getDepartureCity());
                order.setArrivalCity(flight.getArrivalCity());
            }
        } else if (Integer.valueOf(1).equals(order.getOrderType())) {
            Train train = trainMapper.selectById(order.getTicketId());
            if (train != null) {
                order.setDepartureStation(train.getDepartureStation());
                order.setArrivalStation(train.getArrivalStation());
            }
        }
    }
}
