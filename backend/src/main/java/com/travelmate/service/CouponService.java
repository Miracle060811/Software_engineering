package com.travelmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travelmate.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService extends IService<Coupon> {
    /** 列出所有可领取的优惠券 */
    List<Coupon> listAvailable();

    /** 列出当前用户还未领取的可领取优惠券 */
    List<Coupon> listAvailable(Long userId);

    /** 用户领取优惠券，返回领取结果消息 */
    String claimCoupon(Long userId, Long couponId);

    /** 查看用户已领取的优惠券（含详情） */
    List<Map<String, Object>> listMyCoupons(Long userId);

    /** 使用用户优惠券并返回优惠后的金额 */
    BigDecimal useCoupon(Long userId, Long userCouponId, BigDecimal originalAmount);
}
