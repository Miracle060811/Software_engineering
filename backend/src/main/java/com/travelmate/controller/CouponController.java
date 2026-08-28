package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Coupon;
import com.travelmate.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<Coupon>> listAvailable() {
        Long userId = getCurrentUserId();
        return Result.success(couponService.listAvailable(userId));
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
        return userContext.getCurrentUserIdOrNull();
    }
}
