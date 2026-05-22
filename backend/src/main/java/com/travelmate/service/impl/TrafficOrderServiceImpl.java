package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.dto.TrainOrderCreateDTO;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Passenger;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.entity.Train;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.CouponService;
import com.travelmate.service.TrafficOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TrafficOrderServiceImpl extends ServiceImpl<TrafficOrderMapper, TrafficOrder>
        implements TrafficOrderService {

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private PassengerMapper passengerMapper;

    @Autowired
    private CouponService couponService;

    /**
     * 核心难点：防超卖事务拦截、减库存并生成订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createFlightOrder(Long userId, FlightOrderCreateDTO dto) {
        validateFlightOrder(dto);

        // 1. 查询乘车人信息
        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(userId)) {
            throw new RuntimeException("乘车人选错或不存在");
        }

        Flight flight = flightMapper.selectById(dto.getFlightId());
        if (flight == null || flight.getStatus() == null || flight.getStatus() != 1) {
            throw new RuntimeException("航班不存在或已取消");
        }

        // 2. 扣减航班库存 (乐观扣减, 利用 MySQL 行级锁和 where available_seats > 0)
        int updated = flightMapper.deductSeat(dto.getFlightId());
        if (updated == 0) {
            // 抛出异常会自动触发 spring 的事务回滚(@Transactional)
            throw new RuntimeException("库存不足或航班已取消，下单失败 (已超卖)");
        }

        // 3. 获取航班信息用于计算金额
        String seatType = normalizeFlightSeatType(dto.getSeatType());
        BigDecimal price = "Business".equals(seatType)
                ? flight.getBusinessPrice()
                : flight.getEconomyPrice();
        if (price == null) {
            throw new RuntimeException("该舱位暂不可售");
        }
        BigDecimal payableAmount = couponService.useCoupon(userId, dto.getUserCouponId(), price);

        // 4. 构建订单对象并落表生成
        TrafficOrder order = new TrafficOrder();
        // 生成唯一单号 (前缀 T 表示 Ticket + 随机字符串，实际生产可使用雪花算法 Snowflake / Redis Incr)
        String orderNo = "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(0); // 0 代表航班机票
        order.setTicketId(dto.getFlightId());
        order.setSeatType(seatType);
        order.setPassengerName(passenger.getName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setAmount(payableAmount);
        order.setStatus(0); // 0 = 待支付
        order.setCreateTime(LocalDateTime.now());

        save(order); // insert 插入数据

        return order.getOrderNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTrainOrder(Long userId, TrainOrderCreateDTO dto) {
        validateTrainOrder(dto);

        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(userId)) {
            throw new RuntimeException("乘车人选错或不存在");
        }

        Train train = trainMapper.selectById(dto.getTrainId());
        if (train == null || train.getStatus() == null || train.getStatus() != 1) {
            throw new RuntimeException("车次不存在或已停运");
        }

        // 判断席位并进行并发防超卖扣减
        String seatType = normalizeTrainSeatType(dto.getSeatType());
        int updated;
        if ("FirstClass".equals(seatType)) {
            updated = trainMapper.deductFirstClassSeat(dto.getTrainId());
        } else {
            updated = trainMapper.deductSecondClassSeat(dto.getTrainId());
        }

        if (updated == 0) {
            throw new RuntimeException("所选席位已售罄或车次停运");
        }

        BigDecimal price = "FirstClass".equals(seatType)
                ? train.getFirstClassPrice()
                : train.getSecondClassPrice();
        if (price == null) {
            throw new RuntimeException("该席别暂不可售");
        }
        BigDecimal payableAmount = couponService.useCoupon(userId, dto.getUserCouponId(), price);

        TrafficOrder order = new TrafficOrder();
        String orderNo = "TR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(1); // 1 = 火车票
        order.setTicketId(dto.getTrainId());
        order.setSeatType(seatType);
        order.setPassengerName(passenger.getName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setAmount(payableAmount);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());

        save(order);

        return order.getOrderNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long userId, String orderNo) {
        // 先找出这笔订单
        TrafficOrder order = this.getOne(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderNo, orderNo)
                .eq(TrafficOrder::getUserId, userId));

        if (order == null)
            throw new RuntimeException("订单不存在");
        if (order.getStatus() != 0)
            throw new RuntimeException("订单已处理过, 无法再次支付");

        // 模拟支付完成, 原子改为 1-出票中，避免并发重复支付。
        int updated = baseMapper.markPaid(userId, orderNo);
        if (updated == 0) {
            throw new RuntimeException("订单状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long userId, String orderNo) {
        TrafficOrder order = this.getOne(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getOrderNo, orderNo)
                .eq(TrafficOrder::getUserId, userId));

        if (order == null)
            throw new RuntimeException("订单不存在");
        // 只能取消待支付(0)，如果要取消已出票的应该叫 退票(4)
        if (order.getStatus() != 0)
            throw new RuntimeException("非待支付订单无法直接取消");

        // 1. 原子修改订单状态为已取消 (3)，防止重复取消多次归还库存。
        int updated = baseMapper.markCancelledFromPending(userId, orderNo);
        if (updated == 0) {
            throw new RuntimeException("订单状态已变化，请刷新后重试");
        }

        // 2. 归还被锁定的库存(余票 + 1)
        if (order.getOrderType() == 0) {
            flightMapper.returnSeat(order.getTicketId());
        } else if (order.getOrderType() == 1) {
            if ("FirstClass".equalsIgnoreCase(order.getSeatType())) {
                trainMapper.returnFirstClassSeat(order.getTicketId());
            } else {
                trainMapper.returnSecondClassSeat(order.getTicketId());
            }
        }

        return true;
    }

    @Override
    public java.util.List<TrafficOrder> getUserOrders(Long userId) {
        java.util.List<TrafficOrder> orders = this.list(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getUserId, userId)
                .orderByDesc(TrafficOrder::getCreateTime));
        orders.forEach(this::fillTicketSnapshot);
        return orders;
    }

    private void fillTicketSnapshot(TrafficOrder order) {
        if (order.getOrderType() == null || order.getTicketId() == null) {
            return;
        }

        if (order.getOrderType() == 0) {
            Flight flight = flightMapper.selectById(order.getTicketId());
            if (flight != null) {
                order.setTicketNo(flight.getFlightNo());
                order.setDepartureCity(flight.getDepartureCity());
                order.setArrivalCity(flight.getArrivalCity());
            }
            return;
        }

        if (order.getOrderType() == 1) {
            Train train = trainMapper.selectById(order.getTicketId());
            if (train != null) {
                order.setTicketNo(train.getTrainNo());
                order.setDepartureStation(train.getDepartureStation());
                order.setArrivalStation(train.getArrivalStation());
            }
        }
    }

    private void validateFlightOrder(FlightOrderCreateDTO dto) {
        if (dto == null || dto.getFlightId() == null) {
            throw new RuntimeException("请选择航班");
        }
        if (dto.getPassengerId() == null) {
            throw new RuntimeException("请选择乘车人");
        }
        normalizeFlightSeatType(dto.getSeatType());
    }

    private void validateTrainOrder(TrainOrderCreateDTO dto) {
        if (dto == null || dto.getTrainId() == null) {
            throw new RuntimeException("请选择车次");
        }
        if (dto.getPassengerId() == null) {
            throw new RuntimeException("请选择乘车人");
        }
        normalizeTrainSeatType(dto.getSeatType());
    }

    private String normalizeFlightSeatType(String seatType) {
        if (!StringUtils.hasText(seatType)) {
            throw new RuntimeException("请选择舱位");
        }
        if ("Economy".equalsIgnoreCase(seatType)) {
            return "Economy";
        }
        if ("Business".equalsIgnoreCase(seatType)) {
            return "Business";
        }
        throw new RuntimeException("未知的舱位类型");
    }

    private String normalizeTrainSeatType(String seatType) {
        if (!StringUtils.hasText(seatType)) {
            throw new RuntimeException("请选择席别");
        }
        if ("FirstClass".equalsIgnoreCase(seatType)) {
            return "FirstClass";
        }
        if ("SecondClass".equalsIgnoreCase(seatType)) {
            return "SecondClass";
        }
        throw new RuntimeException("未知的席别");
    }
}
