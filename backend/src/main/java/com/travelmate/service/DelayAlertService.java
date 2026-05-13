package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Notification;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 航班延误预警服务（模拟）
 * 定期检查航班并随机生成延误预警通知
 */
@Service
public class DelayAlertService {

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private TrafficOrderMapper trafficOrderMapper;

    @Autowired
    private NotificationCenterService notificationCenterService;

    private final Random random = new Random();

    /**
     * 每60秒检查一次（实际项目中可改为每5分钟或更长）
     */
    @Scheduled(fixedDelay = 60000)
    public void checkAndAlert() {
        try {
            List<Flight> flights = flightMapper.selectList(
                    new LambdaQueryWrapper<Flight>().eq(Flight::getStatus, 1));
            if (flights.isEmpty()) return;

            // 随机选择1-2个航班进行延误模拟
            int count = random.nextInt(2) + 1;
            for (int i = 0; i < count && i < flights.size(); i++) {
                Flight flight = flights.get(random.nextInt(flights.size()));
                int delayMinutes = random.nextInt(120) + 15; // 15-135分钟

                // 查找该航班的所有已出票订单
                List<TrafficOrder> orders = trafficOrderMapper.selectList(
                        new LambdaQueryWrapper<TrafficOrder>()
                                .eq(TrafficOrder::getTicketId, flight.getId())
                                .eq(TrafficOrder::getOrderType, 0)
                                .eq(TrafficOrder::getStatus, 2));

                for (TrafficOrder order : orders) {
                    notificationCenterService.createNotification(
                            order.getUserId(),
                            "system",
                            "航班延误预警",
                            String.format("您的航班 %s（%s→%s）预计延误%d分钟，请提前做好安排。",
                                    flight.getFlightNo(), flight.getDepartureCity(),
                                    flight.getArrivalCity(), delayMinutes));
                }
            }
        } catch (Exception ignored) {
            // 静默处理，不影响主业务
        }
    }
}
