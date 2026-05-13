package com.travelmate.controller;

import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Coupon;
import com.travelmate.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/list")
    public Result<List<Coupon>> listAvailable() {
        return Result.success(couponService.listAvailable());
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myCoupons() {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("请先登录");
        return Result.success(couponService.listMyCoupons(userId));
    }

    @PostMapping("/claim/{id}")
    public Result<String> claim(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error("请先登录");
        try {
            return Result.success(couponService.claimCoupon(userId, id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        String username = auth.getName();
        if ("anonymousUser".equals(username))
            return null;
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
