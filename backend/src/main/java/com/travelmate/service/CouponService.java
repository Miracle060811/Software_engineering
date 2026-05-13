package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Coupon;

import java.util.List;
import java.util.Map;

public interface CouponService extends IService<Coupon> {
    /** 列出所有可领取的优惠券 */
    List<Coupon> listAvailable();

    /** 用户领取优惠券，返回领取结果消息 */
    String claimCoupon(Long userId, Long couponId);

    /** 查看用户已领取的优惠券（含详情） */
    List<Map<String, Object>> listMyCoupons(Long userId);
}
