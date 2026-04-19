package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.dto.TrainOrderCreateDTO;
import com.travelmate.service.TrafficOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 成员A负责: 订单核心与防超卖机制
 */
@RestController
@RequestMapping("/api/order")
public class TrafficOrderController {

    @Autowired
    private TrafficOrderService trafficOrderService;

    /**
     * 1. [核心] 生成机票订单, 会预扣减库存
     */
    @PostMapping("/flight/create")
    public Result<String> createFlightOrder(@RequestBody FlightOrderCreateDTO dto) {
        Long currentUserId = 1L; // TODO: 这里为了演示先写死, 以后改成从 Headers Token/Session 中拿
        try {
            String orderNo = trafficOrderService.createFlightOrder(currentUserId, dto);
            return Result.success(orderNo);
        } catch (Exception e) {
            // 返回友好的报错信息 (比如 "库存不足或航班已取消")
            return Result.error(e.getMessage());
        }
    }

    /**
     * 1.1 生成火车票订单, 会预扣减库存
     */
    @PostMapping("/train/create")
    public Result<String> createTrainOrder(@RequestBody TrainOrderCreateDTO dto) {
        Long currentUserId = 1L; // 模拟当前用户
        try {
            String orderNo = trafficOrderService.createTrainOrder(currentUserId, dto);
            return Result.success(orderNo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 2. [核心] 用户端模拟点击支付按钮 (或回调接口)
     */
    @PostMapping("/pay/{orderNo}")
    public Result<String> mockPay(@PathVariable String orderNo) {
        Long currentUserId = 1L;
        try {
            boolean success = trafficOrderService.payOrder(currentUserId, orderNo);
            return success ? Result.success("支付成功, 正在出票") : Result.error("支付异常");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 3. 取消未支付订单, 释放库存座位返回给池子
     */
    @PostMapping("/cancel/{orderNo}")
    public Result<String> cancelOrder(@PathVariable String orderNo) {
        Long currentUserId = 1L;
        try {
            boolean success = trafficOrderService.cancelOrder(currentUserId, orderNo);
            return success ? Result.success("已取消该订单, 座位已归还") : Result.error("取消失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 4. 获取用户个人的订单列表
     */
    @GetMapping("/list")
    public Result<java.util.List<com.travelmate.entity.TrafficOrder>> getMyOrders() {
        Long currentUserId = 1L; // 模拟当前登录用户
        return Result.success(trafficOrderService.getUserOrders(currentUserId));
    }
}
