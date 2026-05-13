package com.travelmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travelmate.entity.Coupon;
import com.travelmate.entity.UserCoupon;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.UserCouponMapper;
import com.travelmate.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> listAvailable() {
        return list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 0)
                .gt(Coupon::getStock, 0)
                .ge(Coupon::getExpireDate, LocalDateTime.now())
                .orderByDesc(Coupon::getCreateTime));
    }

    @Override
    @Transactional
    public String claimCoupon(Long userId, Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null)
            throw new RuntimeException("优惠券不存在");
        if (coupon.getStatus() != 0)
            throw new RuntimeException("该优惠券已失效");
        if (coupon.getStock() <= 0)
            throw new RuntimeException("该优惠券已被领完");
        if (coupon.getExpireDate() != null && coupon.getExpireDate().isBefore(LocalDateTime.now()))
            throw new RuntimeException("该优惠券已过期");

        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (count > 0)
            throw new RuntimeException("已领取过该优惠券");

        coupon.setStock(coupon.getStock() - 1);
        updateById(coupon);

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setReceivedTime(LocalDateTime.now());
        userCouponMapper.insert(uc);

        return "领取成功";
    }

    @Override
    public List<Map<String, Object>> listMyCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getReceivedTime));

        if (userCoupons.isEmpty()) return new ArrayList<>();

        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = listByIds(couponIds);
        Map<Long, Coupon> couponMap = coupons.stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", uc.getId());
            item.put("status", uc.getStatus());
            item.put("receivedTime", uc.getReceivedTime());
            item.put("usedTime", uc.getUsedTime());

            Coupon c = couponMap.get(uc.getCouponId());
            if (c != null) {
                item.put("couponName", c.getName());
                item.put("description", c.getDescription());
                item.put("discountType", c.getDiscountType());
                item.put("discountValue", c.getDiscountValue());
                item.put("minAmount", c.getMinAmount());
                item.put("expireDate", c.getExpireDate());
            }
            result.add(item);
        }
        return result;
    }
}
