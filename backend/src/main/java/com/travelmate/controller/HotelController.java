package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.dto.HotelOrderCreateDTO;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.service.HotelOrderService;
import com.travelmate.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.travelmate.annotation.RateLimiter;

/**
 * 酒店住宿控制器 (成员B负责)
 */
@RestController
@RequestMapping("/api/hotel")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private HotelOrderService hotelOrderService;

    @Autowired
    private UserMapper userMapper;

    // ===================== 酒店查询 =====================

    /**
     * 搜索酒店
     * GET
     * /api/hotel/search?city=&checkIn=&checkOut=&starRating=&minPrice=&maxPrice=
     */
    @GetMapping("/search")
    public Result<List<Hotel>> searchHotels(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        List<Hotel> hotels = hotelService.searchHotels(city, checkIn, checkOut,
                starRating, minPrice, maxPrice);
        return Result.success(hotels);
    }

    /**
     * 获取酒店详情（含房型列表）
     * GET /api/hotel/{id}
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getHotelDetail(@PathVariable Long id) {
        try {
            Map<String, Object> detail = hotelService.getHotelWithRooms(id);
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取酒店房型列表
     * GET /api/hotel/{id}/rooms
     */
    @GetMapping("/{id}/rooms")
    public Result<List<HotelRoom>> getRooms(@PathVariable Long id) {
        List<HotelRoom> rooms = hotelService.getRoomsByHotelId(id);
        return Result.success(rooms);
    }

    // ===================== 酒店订单 =====================

    /**
     * 创建酒店订单（需要登录认证）
     * POST /api/hotel/order/create
     */
    @RateLimiter(maxRequests = 3, timeWindowSeconds = 1)
    @PostMapping("/order/create")
    public Result<String> createOrder(@RequestBody HotelOrderCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        try {
            String orderNo = hotelOrderService.createOrder(userId, dto);
            return Result.success(orderNo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 模拟支付订单
     * POST /api/hotel/order/{orderNo}/pay
     */
    @RateLimiter(maxRequests = 5, timeWindowSeconds = 1)
    @PostMapping("/order/{orderNo}/pay")
    public Result<String> payOrder(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        try {
            boolean success = hotelOrderService.payOrder(userId, orderNo);
            return success ? Result.success("支付成功") : Result.error("支付失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     * POST /api/hotel/order/{orderNo}/cancel
     */
    @PostMapping("/order/{orderNo}/cancel")
    public Result<String> cancelOrder(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        try {
            boolean success = hotelOrderService.cancelOrder(userId, orderNo);
            return success ? Result.success("订单已取消") : Result.error("取消失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的酒店订单列表
     * GET /api/hotel/orders
     */
    @GetMapping("/orders")
    public Result<List<HotelOrder>> getMyOrders() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        return Result.success(hotelOrderService.getUserOrders(userId));
    }

    // ===================== 工具方法 =====================

    /**
     * 从 SecurityContext 获取当前登录用户的 userId
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
