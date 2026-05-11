package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.dto.HotelOrderCreateDTO;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.HotelOrderService;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.NotificationCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HotelOrderServiceImpl extends ServiceImpl<HotelOrderMapper, HotelOrder>
        implements HotelOrderService {

    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private HotelRoomStockService hotelRoomStockService;

    @Autowired
    private NotificationCenterService notificationCenterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, HotelOrderCreateDTO dto) {
        // 1. 检查日期合法性
        LocalDate checkIn = dto.getCheckInDate();
        LocalDate checkOut = dto.getCheckOutDate();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new RuntimeException("入住/退房日期不合法");
        }

        // 2. 查询房型信息
        HotelRoom room = hotelRoomMapper.selectById(dto.getRoomId());
        if (room == null || room.getStatus() != 1) {
            throw new RuntimeException("房型不存在或已下线");
        }
        if (!room.getHotelId().equals(dto.getHotelId())) {
            throw new RuntimeException("房型与酒店不匹配");
        }

        boolean redisPreDeducted = hotelRoomStockService.preDeductRoom(dto.getRoomId(), room.getAvailableRooms());
        if (!redisPreDeducted) {
            throw new RuntimeException("该房型暂无可用房间，预订失败");
        }

        try {
            int updated = hotelRoomMapper.deductRoom(dto.getRoomId());
            if (updated == 0) {
                throw new RuntimeException("该房型暂无可用房间，预订失败");
            }

            Hotel hotel = hotelMapper.selectById(dto.getHotelId());
            long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
            BigDecimal amount = room.getPrice().multiply(BigDecimal.valueOf(nights));

            HotelOrder order = new HotelOrder();
            String orderNo = "HT" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setHotelId(dto.getHotelId());
            order.setRoomId(dto.getRoomId());
            order.setHotelName(hotel != null ? hotel.getName() : "");
            order.setRoomType(room.getRoomType());
            order.setCheckInDate(checkIn);
            order.setCheckOutDate(checkOut);
            order.setNights((int) nights);
            order.setGuestName(dto.getGuestName());
            order.setGuestPhone(dto.getGuestPhone());
            order.setAmount(amount);
            order.setStatus(0);
            order.setCreateTime(LocalDateTime.now());

            if (!save(order)) {
                throw new RuntimeException("订单创建失败，请稍后重试");
            }

            notificationCenterService.createNotification(
                    userId,
                    "hotel_order",
                    "酒店订单已创建",
                    String.format("您的酒店订单 %s 已创建，请在15分钟内完成支付。", orderNo));

            System.out.println("====== [HotelOrder] 订单创建成功: " + orderNo + " ======");
            return orderNo;
        } catch (Exception e) {
            hotelRoomStockService.rollbackPreDeduct(dto.getRoomId());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long userId, String orderNo) {
        HotelOrder order = getOne(new LambdaQueryWrapper<HotelOrder>()
                .eq(HotelOrder::getOrderNo, orderNo)
                .eq(HotelOrder::getUserId, userId));

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态异常，无法支付");
        }

        order.setStatus(1); // 1 = 已支付
        order.setPayTime(LocalDateTime.now());
        boolean success = updateById(order);

        if (success) {
            notificationCenterService.createNotification(
                    userId,
                    "hotel_order",
                    "酒店订单支付成功",
                    String.format("订单 %s 已支付成功，祝您旅途愉快。", orderNo));
        }

        System.out.println("====== [HotelOrder] 订单支付成功: " + orderNo + " ======");
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long userId, String orderNo) {
        HotelOrder order = getOne(new LambdaQueryWrapper<HotelOrder>()
                .eq(HotelOrder::getOrderNo, orderNo)
                .eq(HotelOrder::getUserId, userId));

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("只有待支付的订单才能取消");
        }

        // 1. 修改订单状态为已取消
        order.setStatus(4); // 4 = 已取消
        updateById(order);

        // 2. 归还房间库存
        hotelRoomMapper.returnRoom(order.getRoomId());
        hotelRoomStockService.syncWithDatabase(order.getRoomId());
        notificationCenterService.createNotification(
                userId,
                "hotel_order",
                "酒店订单已取消",
                String.format("订单 %s 已取消，库存已自动归还。", orderNo));

        System.out.println("====== [HotelOrder] 订单已取消，房间已归还: " + orderNo + " ======");
        return true;
    }

    @Override
    public List<HotelOrder> getUserOrders(Long userId) {
        return list(new LambdaQueryWrapper<HotelOrder>()
                .eq(HotelOrder::getUserId, userId)
                .orderByDesc(HotelOrder::getCreateTime));
    }
}
