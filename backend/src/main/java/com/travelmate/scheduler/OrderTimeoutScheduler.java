package com.travelmate.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.NotificationCenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(
        name = "app.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderTimeoutScheduler {

    private static final int TIMEOUT_MINUTES = 15;
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Autowired
    private TrafficOrderMapper trafficOrderMapper;

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Autowired
    private HotelRoomStockService hotelRoomStockService;

    @Autowired
    private NotificationCenterService notificationCenterService;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 每5分钟执行一次
    public void cancelTimeoutOrders() {
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

            // 处理大交通超时订单
            LambdaQueryWrapper<TrafficOrder> trafficQuery = new LambdaQueryWrapper<>();
            trafficQuery.eq(TrafficOrder::getStatus, 0)
                    .lt(TrafficOrder::getCreateTime, deadline);
            List<TrafficOrder> timeoutTrafficOrders = trafficOrderMapper.selectList(trafficQuery);
            for (TrafficOrder order : timeoutTrafficOrders) {
                int updated = trafficOrderMapper.markCancelledFromPending(order.getUserId(), order.getOrderNo());
                if (updated == 0) {
                    continue;
                }
                returnTrafficStock(order);
                notificationCenterService.createNotification(
                        order.getUserId(),
                        "traffic_order",
                        "票务订单超时取消",
                        String.format("订单 %s 超过15分钟未支付，系统已自动取消并释放库存。", order.getOrderNo()),
                        "/my-orders?tab=traffic");
                log.info("[Timeout] 大交通订单超时取消并归还库存: {}", order.getOrderNo());
            }

            // 处理酒店超时订单
            LambdaQueryWrapper<HotelOrder> hotelQuery = new LambdaQueryWrapper<>();
            hotelQuery.eq(HotelOrder::getStatus, 0)
                    .lt(HotelOrder::getCreateTime, deadline);
            List<HotelOrder> timeoutHotelOrders = hotelOrderMapper.selectList(hotelQuery);
            for (HotelOrder order : timeoutHotelOrders) {
                int updated = hotelOrderMapper.markCancelledFromPending(order.getUserId(), order.getOrderNo());
                if (updated == 0) {
                    continue;
                }
                // 归还房间库存
                hotelRoomMapper.returnRoom(order.getRoomId(), order.getRoomCount() == null ? 1 : order.getRoomCount());
                hotelRoomStockService.syncWithDatabase(order.getRoomId());
                notificationCenterService.createNotification(
                        order.getUserId(),
                        "hotel_order",
                        "酒店订单超时取消",
                        String.format("订单 %s 超过15分钟未支付，系统已自动取消。", order.getOrderNo()),
                        "/my-orders?tab=hotel");
                log.info("[Timeout] 酒店订单超时取消: {}", order.getOrderNo());
            }
        } catch (RuntimeException e) {
            if (isDatabaseUnavailable(e)) {
                log.warn("跳过订单超时扫描：数据库当前不可用。{}", getRootMessage(e));
                return;
            }
            throw e;
        }
    }

    private void returnTrafficStock(TrafficOrder order) {
        if (order.getOrderType() == null) {
            return;
        }

        if (order.getOrderType() == 0) {
            flightMapper.returnSeat(order.getTicketId(), getTicketCount(order));
            return;
        }

        if (order.getOrderType() == 1) {
            if ("FirstClass".equalsIgnoreCase(order.getSeatType())) {
                trainMapper.returnFirstClassSeat(order.getTicketId(), getTicketCount(order));
            } else {
                trainMapper.returnSecondClassSeat(order.getTicketId(), getTicketCount(order));
            }
        }
    }

    private int getTicketCount(TrafficOrder order) {
        return order.getTicketCount() == null ? 1 : order.getTicketCount();
    }

    private boolean isDatabaseUnavailable(Throwable throwable) {
        Throwable root = getRootCause(throwable);
        String rootMessage = root.getMessage();

        return throwable instanceof CannotGetJdbcConnectionException
                || root instanceof CannotGetJdbcConnectionException
                || contains(rootMessage, "Access denied for user")
                || contains(rootMessage, "Communications link failure")
                || contains(rootMessage, "Failed to obtain JDBC Connection");
    }

    private String getRootMessage(Throwable throwable) {
        String message = getRootCause(throwable).getMessage();
        return message == null || message.isBlank() ? "请检查 MySQL 是否启动以及数据库密码配置。" : message;
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }
}
