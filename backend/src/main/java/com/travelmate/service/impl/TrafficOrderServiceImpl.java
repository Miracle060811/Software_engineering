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
import com.travelmate.service.TrafficOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 核心难点：防超卖事务拦截、减库存并生成订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createFlightOrder(Long userId, FlightOrderCreateDTO dto) {

        // 1. 查询乘车人信息
        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(userId)) {
            throw new RuntimeException("乘车人选错或不存在");
        }

        // 2. 扣减航班库存 (乐观扣减, 利用 MySQL 行级锁和 where available_seats > 0)
        int updated = flightMapper.deductSeat(dto.getFlightId());
        if (updated == 0) {
            // 抛出异常会自动触发 spring 的事务回滚(@Transactional)
            throw new RuntimeException("库存不足或航班已取消，下单失败 (已超卖)");
        }

        // 3. 获取航班信息用于计算金额
        Flight flight = flightMapper.selectById(dto.getFlightId());
        BigDecimal price = "Business".equalsIgnoreCase(dto.getSeatType())
                ? flight.getBusinessPrice()
                : flight.getEconomyPrice();

        // 4. 构建订单对象并落表生成
        TrafficOrder order = new TrafficOrder();
        // 生成唯一单号 (前缀 T 表示 Ticket + 随机字符串，实际生产可使用雪花算法 Snowflake / Redis Incr)
        String orderNo = "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(0); // 0 代表航班机票
        order.setTicketId(dto.getFlightId());
        order.setSeatType(dto.getSeatType());
        order.setPassengerName(passenger.getName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setAmount(price);
        order.setStatus(0); // 0 = 待支付
        order.setCreateTime(LocalDateTime.now());

        System.out.println("====== [Order DEBUG] 成功扣减库存, 正在生成订单: " + orderNo + " ======");
        save(order); // insert 插入数据

        return order.getOrderNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTrainOrder(Long userId, TrainOrderCreateDTO dto) {
        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(userId)) {
            throw new RuntimeException("乘车人选错或不存在");
        }

        // 判断席位并进行并发防超卖扣减
        int updated = 0;
        if ("FirstClass".equalsIgnoreCase(dto.getSeatType())) {
            updated = trainMapper.deductFirstClassSeat(dto.getTrainId());
        } else if ("SecondClass".equalsIgnoreCase(dto.getSeatType())) {
            updated = trainMapper.deductSecondClassSeat(dto.getTrainId());
        } else {
            throw new RuntimeException("未知的席别");
        }

        if (updated == 0) {
            throw new RuntimeException("所选席位已售罄或车次停运");
        }

        Train train = trainMapper.selectById(dto.getTrainId());
        BigDecimal price = "FirstClass".equalsIgnoreCase(dto.getSeatType())
                ? train.getFirstClassPrice()
                : train.getSecondClassPrice();

        TrafficOrder order = new TrafficOrder();
        String orderNo = "H" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderType(1); // 1 = 火车票
        order.setTicketId(dto.getTrainId());
        order.setSeatType(dto.getSeatType());
        order.setPassengerName(passenger.getName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setAmount(price);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());

        System.out.println("====== [Order DEBUG] 成功扣减火车票库存, 生成订单: " + orderNo + " ======");
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

        // 模拟支付完成, 改为 1-出票中 (有些会直接跳 2-已出票, 这里模拟先出票中)
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        boolean success = updateById(order);
        System.out.println("====== [Order DEBUG] 订单 " + orderNo + " 模拟支付成功, 开始出票... ======");
        return success;
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

        // 1. 修改订单状态为已取消 (3)
        order.setStatus(3);
        updateById(order);

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

        System.out.println("====== [Order DEBUG] 订单 " + orderNo + " 已取消，成功归还座位！ ======");

        return true;
    }

    @Override
    public java.util.List<TrafficOrder> getUserOrders(Long userId) {
        return this.list(new LambdaQueryWrapper<TrafficOrder>()
                .eq(TrafficOrder::getUserId, userId)
                .orderByDesc(TrafficOrder::getCreateTime));
    }
}
