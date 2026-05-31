package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.AttractionOrder;
import com.travelmate.service.AttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 景点门票控制器 (成员B负责)
 */
@RestController
@RequestMapping("/api/attraction")
public class AttractionController {

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 搜索景点
     * GET /api/attraction/search?city=
     */
    @GetMapping("/search")
    public Result<List<Attraction>> searchAttractions(
            @RequestParam(required = false) String city) {
        return Result.success(attractionService.searchAttractions(city));
    }

    /**
     * 景点详情
     * GET /api/attraction/{id}
     */
    @GetMapping("/{id}")
    public Result<Attraction> getDetail(@PathVariable Long id) {
        Attraction attraction = attractionService.getById(id);
        if (attraction == null || attraction.getStatus() != 1) {
            return Result.error("景点不存在或已下线");
        }
        return Result.success(attraction);
    }

    /**
     * 购买门票
     * POST /api/attraction/{id}/ticket
     * Body: { "adultCount": 1, "childCount": 1, "guestName": "张三", "guestPhone": "13800138000" }
     */
    @PostMapping("/{id}/ticket")
    public Result<String> buyTicket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }

        Integer adultCount = body.get("adultCount") != null
                ? Integer.valueOf(body.get("adultCount").toString())
                : (body.get("count") != null ? Integer.valueOf(body.get("count").toString()) : 1);
        Integer childCount = body.get("childCount") != null
                ? Integer.valueOf(body.get("childCount").toString())
                : 0;
        String guestName = body.get("guestName") != null
                ? body.get("guestName").toString()
                : "";
        String guestPhone = body.get("guestPhone") != null
                ? body.get("guestPhone").toString()
                : "";

        try {
            String orderNo = attractionService.buyTicket(userId, id, adultCount, childCount, guestName, guestPhone);
            return Result.success(orderNo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的景点门票订单
     * GET /api/attraction/orders
     */
    @GetMapping("/orders")
    public Result<List<AttractionOrder>> getMyTicketOrders() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        return Result.success(attractionService.getUserTicketOrders(userId));
    }

    /**
     * 景点门票订单详情/核销凭证
     * GET /api/attraction/order/{orderNo}/receipt
     */
    @GetMapping("/order/{orderNo}/receipt")
    public Result<AttractionOrder> getTicketReceipt(@PathVariable String orderNo) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        AttractionOrder order = attractionService.getTicketOrderDetail(userId, orderNo);
        return order == null ? Result.error("订单不存在或无权查看") : Result.success(order);
    }

    // ===================== 工具方法 =====================

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
