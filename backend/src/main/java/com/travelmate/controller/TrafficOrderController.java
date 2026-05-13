package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.dto.TrainOrderCreateDTO;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.service.TrafficOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.travelmate.annotation.RateLimiter;

/**
 * 成员A负责: 订单核心与防超卖机制
 */
@RestController
@RequestMapping("/api/order")
public class TrafficOrderController {

    @Autowired
    private TrafficOrderService trafficOrderService;

    @Autowired
    private UserMapper userMapper;

    @RateLimiter(maxRequests = 3, timeWindowSeconds = 1)
    @PostMapping("/flight/create")
    public Result<String> createFlightOrder(@RequestBody FlightOrderCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        try {
            return Result.success(trafficOrderService.createFlightOrder(userId, dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @RateLimiter(maxRequests = 3, timeWindowSeconds = 1)
    @PostMapping("/train/create")
    public Result<String> createTrainOrder(@RequestBody TrainOrderCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        try {
            return Result.success(trafficOrderService.createTrainOrder(userId, dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @RateLimiter(maxRequests = 5, timeWindowSeconds = 1)
    @PostMapping("/pay/{orderNo}")
    public Result<String> mockPay(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        try {
            boolean ok = trafficOrderService.payOrder(userId, orderNo);
            return ok ? Result.success("支付成功，正在出票") : Result.error("支付异常");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{orderNo}")
    public Result<String> cancelOrder(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        try {
            boolean ok = trafficOrderService.cancelOrder(userId, orderNo);
            return ok ? Result.success("已取消，座位已归还") : Result.error("取消失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<TrafficOrder>> getMyOrders() {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        return Result.success(trafficOrderService.getUserOrders(userId));
    }

    /**
     * 获取单个订单详情（行程单/收据基础数据）
     */
    @GetMapping("/{orderNo}/receipt")
    public Result<TrafficOrder> getReceipt(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("用户未登录");
        List<TrafficOrder> orders = trafficOrderService.getUserOrders(userId);
        TrafficOrder order = orders.stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst()
                .orElse(null);
        if (order == null)
            return Result.error("订单不存在或无权查看");
        return Result.success(order);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        String username = auth.getName();
        if ("anonymousUser".equals(username))
            return null;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
