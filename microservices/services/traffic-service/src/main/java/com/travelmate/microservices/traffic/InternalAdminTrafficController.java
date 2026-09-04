package com.travelmate.microservices.traffic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Train;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.integration.NotificationGateway;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/traffic/admin")
public class InternalAdminTrafficController {
    private final FlightMapper flightMapper;
    private final TrainMapper trainMapper;
    private final TrafficOrderMapper orderMapper;
    private final NotificationGateway notificationGateway;
    private final String token;

    public InternalAdminTrafficController(FlightMapper flightMapper, TrainMapper trainMapper, TrafficOrderMapper orderMapper,
                                          NotificationGateway notificationGateway,
                                          @Value("${app.internal-service-token}") String token) {
        this.flightMapper = flightMapper;
        this.trainMapper = trainMapper;
        this.orderMapper = orderMapper;
        this.notificationGateway = notificationGateway;
        this.token = token;
    }

    @GetMapping("/flights")
    public List<Flight> flights(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return flightMapper.selectList(new LambdaQueryWrapper<Flight>().orderByDesc(Flight::getDepartureTime));
    }

    @PostMapping("/flights")
    public Flight addFlight(@RequestBody Flight flight, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        validateFlight(flight);
        flight.setId(null);
        flightMapper.insert(flight);
        return flight;
    }

    @PutMapping("/flights/{id}")
    public void updateFlight(@PathVariable Long id, @RequestBody Flight flight,
                             @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        validateFlight(flight);
        flight.setId(id);
        if (flightMapper.updateById(flight) == 0) notFound("航班不存在");
    }

    @DeleteMapping("/flights/{id}")
    public void deleteFlight(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 0).eq(TrafficOrder::getTicketId, id));
        if (orderCount > 0) conflict("该航班已有订单，不能删除");
        if (flightMapper.deleteById(id) == 0) notFound("航班不存在");
    }

    @GetMapping("/trains")
    public List<Train> trains(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return trainMapper.selectList(new LambdaQueryWrapper<Train>().orderByDesc(Train::getDepartureTime));
    }

    @PostMapping("/trains")
    public Train addTrain(@RequestBody Train train, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        validateTrain(train);
        train.setId(null);
        trainMapper.insert(train);
        return train;
    }

    @PutMapping("/trains/{id}")
    public void updateTrain(@PathVariable Long id, @RequestBody Train train,
                            @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        validateTrain(train);
        train.setId(id);
        if (trainMapper.updateById(train) == 0) notFound("车次不存在");
    }

    @DeleteMapping("/trains/{id}")
    public void deleteTrain(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderType, 1).eq(TrafficOrder::getTicketId, id));
        if (orderCount > 0) conflict("该车次已有订单，不能删除");
        if (trainMapper.deleteById(id) == 0) notFound("车次不存在");
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

    @PostMapping("/orders/{orderNo}/refund/approve")
    @Transactional(rollbackFor = Exception.class)
    public String approveRefund(@PathVariable String orderNo, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        TrafficOrder order = findOrder(orderNo);
        if (Integer.valueOf(4).equals(order.getStatus())) return "订单已退票，无需重复处理";
        if (Integer.valueOf(3).equals(order.getStatus())) conflict("已取消的待支付订单不能再次退款");
        if (!Integer.valueOf(5).equals(order.getStatus())) conflict("只有已提交退票申请的订单可以办理退票");
        if (orderMapper.markRefundApproved(orderNo) == 0) conflict("订单状态已变化，请刷新后重试");
        returnStock(order);
        notificationGateway.publish(order.getUserId(), "traffic_order", "退票申请已通过",
                "订单 " + orderNo + " 已退票，库存已归还。", "/my-orders?tab=traffic");
        return "退款审批已通过";
    }

    @PostMapping("/orders/{orderNo}/refund/reject")
    @Transactional(rollbackFor = Exception.class)
    public String rejectRefund(@PathVariable String orderNo, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        TrafficOrder order = findOrder(orderNo);
        if (orderMapper.markRefundRejected(orderNo) == 0) conflict("只有退票申请中的订单可以驳回");
        notificationGateway.publish(order.getUserId(), "traffic_order", "退票申请被驳回",
                "订单 " + orderNo + " 未通过退票审核，订单已恢复为已出票。", "/my-orders?tab=traffic");
        return "退款申请已驳回";
    }

    @PostMapping("/orders/{orderNo}/ticket/complete")
    @Transactional(rollbackFor = Exception.class)
    public String completeTicket(@PathVariable String orderNo, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        TrafficOrder order = findOrder(orderNo);
        if (Integer.valueOf(2).equals(order.getStatus())) return "订单已完成出票，无需重复处理";
        if (!Integer.valueOf(1).equals(order.getStatus())) conflict("只有出票中的交通订单可以完成出票");
        if (orderMapper.markTicketed(orderNo) == 0) conflict("订单状态已变化，请刷新后重试");
        notificationGateway.publish(order.getUserId(), "traffic_order",
                Integer.valueOf(0).equals(order.getOrderType()) ? "机票已出票" : "火车票已出票",
                "订单 " + orderNo + " 已完成出票，可在订单详情中查看。", "/my-orders?tab=traffic");
        return "出票已完成";
    }

    private void validateFlight(Flight flight) {
        requireText(flight.getFlightNo(), "航班号");
        requireText(flight.getAirline(), "航司");
        requireText(flight.getDepartureCity(), "出发城市");
        requireText(flight.getArrivalCity(), "到达城市");
        requireTimeRange(flight.getDepartureTime(), flight.getArrivalTime());
        requireNonNegative(flight.getEconomyPrice(), "经济舱价格");
        requireNonNegative(flight.getBusinessPrice(), "公务舱价格");
        requireNonNegative(flight.getTotalSeats(), "总座位数");
        requireNonNegative(flight.getAvailableSeats(), "可售座位数");
        if (flight.getAvailableSeats() > flight.getTotalSeats()) badRequest("可售座位数不能大于总座位数");
    }

    private void validateTrain(Train train) {
        requireText(train.getTrainNo(), "车次");
        requireText(train.getTrainType(), "车型");
        requireText(train.getDepartureStation(), "出发站");
        requireText(train.getArrivalStation(), "到达站");
        requireTimeRange(train.getDepartureTime(), train.getArrivalTime());
        requireNonNegative(train.getFirstClassPrice(), "一等座价格");
        requireNonNegative(train.getSecondClassPrice(), "二等座价格");
        requireNonNegative(train.getFirstClassSeats(), "一等座余票");
        requireNonNegative(train.getSecondClassSeats(), "二等座余票");
        if (train.getDurationMinutes() == null) {
            train.setDurationMinutes((int) Duration.between(train.getDepartureTime(), train.getArrivalTime()).toMinutes());
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) badRequest(name + "不能为空");
    }

    private void requireNonNegative(Number value, String name) {
        if (value == null || Double.compare(value.doubleValue(), 0D) < 0) badRequest(name + "不能为负数");
    }

    private void requireTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) badRequest("出发/到达时间不能为空");
        if (!end.isAfter(start)) badRequest("到达时间必须晚于出发时间");
    }

    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void notFound(String message) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }

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

    private TrafficOrder findOrder(String orderNo) {
        TrafficOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) notFound("订单不存在");
        return order;
    }

    private void returnStock(TrafficOrder order) {
        int count = order.getTicketCount() == null ? 1 : order.getTicketCount();
        if (Integer.valueOf(0).equals(order.getOrderType())) {
            flightMapper.returnSeat(order.getTicketId(), count);
        } else if (Integer.valueOf(1).equals(order.getOrderType())) {
            if ("FirstClass".equalsIgnoreCase(order.getSeatType())) trainMapper.returnFirstClassSeat(order.getTicketId(), count);
            else trainMapper.returnSecondClassSeat(order.getTicketId(), count);
        }
    }
}
