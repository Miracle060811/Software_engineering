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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> listAvailable() {
        List<Coupon> coupons = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 0)
                .gt(Coupon::getStock, 0)
                .and(wrapper -> wrapper.isNull(Coupon::getExpireDate)
                        .or()
                        .ge(Coupon::getExpireDate, LocalDateTime.now()))
                .orderByDesc(Coupon::getCreateTime));
        return distinctByCouponRule(coupons);
    }

    @Override
    public List<Coupon> listAvailable(Long userId) {
        List<Coupon> coupons = listAvailable();
        if (userId == null || coupons.isEmpty()) {
            return coupons;
        }

        List<UserCoupon> claimed = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId));
        Set<String> claimedCouponRules = getClaimedCouponRules(claimed);
        return coupons.stream()
                .filter(coupon -> !claimedCouponRules.contains(couponRuleKey(coupon)))
                .collect(Collectors.toList());
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

        List<UserCoupon> claimed = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId));
        if (getClaimedCouponRules(claimed).contains(couponRuleKey(coupon)))
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
                item.put("couponId", c.getId());
                item.put("couponName", c.getName());
                item.put("description", c.getDescription());
                item.put("category", normalizeCategory(c.getCategory()));
                item.put("discountType", c.getDiscountType());
                item.put("discountValue", c.getDiscountValue());
                item.put("minAmount", c.getMinAmount());
                item.put("expireDate", c.getExpireDate());
                if (uc.getStatus() == 0 && c.getExpireDate() != null && c.getExpireDate().isBefore(LocalDateTime.now())) {
                    item.put("status", 2);
                }
            }
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long userId, Long userCouponId, BigDecimal originalAmount, String orderCategory) {
        if (userCouponId == null) {
            return normalizeAmount(originalAmount);
        }
        BigDecimal amount = normalizeAmount(originalAmount);
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new RuntimeException("优惠券不存在或无权使用");
        }
        if (userCoupon.getStatus() == null || userCoupon.getStatus() != 0) {
            throw new RuntimeException("优惠券已使用或已失效");
        }

        Coupon coupon = getById(userCoupon.getCouponId());
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 0) {
            throw new RuntimeException("优惠券已失效");
        }
        if (coupon.getExpireDate() != null && coupon.getExpireDate().isBefore(LocalDateTime.now())) {
            userCouponMapper.markExpired(userCouponId, userId);
            throw new RuntimeException("优惠券已过期");
        }
        if (!isCategoryAllowed(coupon.getCategory(), orderCategory)) {
            throw new RuntimeException("该优惠券不适用于当前订单类型");
        }
        if (coupon.getMinAmount() != null && amount.compareTo(coupon.getMinAmount()) < 0) {
            throw new RuntimeException("订单金额未达到优惠券使用门槛");
        }

        BigDecimal finalAmount;
        if (coupon.getDiscountType() != null && coupon.getDiscountType() == 1) {
            finalAmount = amount.multiply(coupon.getDiscountValue());
        } else {
            finalAmount = amount.subtract(coupon.getDiscountValue());
        }
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        int updated = userCouponMapper.markUsed(userCouponId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new RuntimeException("优惠券状态已变化，请刷新后重试");
        }
        return normalizeAmount(finalAmount);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Coupon> distinctByCouponRule(List<Coupon> coupons) {
        Set<String> seen = new HashSet<>();
        List<Coupon> result = new ArrayList<>();
        for (Coupon coupon : coupons) {
            String key = couponRuleKey(coupon);
            if (seen.add(key)) {
                result.add(coupon);
            }
        }
        return result;
    }

    private Set<String> getClaimedCouponRules(List<UserCoupon> userCoupons) {
        if (userCoupons == null || userCoupons.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toList());
        return listByIds(couponIds).stream()
                .map(this::couponRuleKey)
                .collect(Collectors.toSet());
    }

    private String couponRuleKey(Coupon coupon) {
        if (coupon == null) {
            return "";
        }
        return String.join("|",
                String.valueOf(coupon.getName()),
                String.valueOf(coupon.getDescription()),
                normalizeCategory(coupon.getCategory()),
                String.valueOf(coupon.getDiscountType()),
                normalizeDecimal(coupon.getDiscountValue()),
                normalizeDecimal(coupon.getMinAmount()));
    }

    private boolean isCategoryAllowed(String couponCategory, String orderCategory) {
        String normalizedCouponCategory = normalizeCategory(couponCategory);
        String normalizedOrderCategory = normalizeCategory(orderCategory);
        return "all".equals(normalizedCouponCategory)
                || normalizedCouponCategory.equals(normalizedOrderCategory);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "all";
        }
        String value = category.trim().toLowerCase();
        if ("flight".equals(value) || "train".equals(value) || "hotel".equals(value)) {
            return value;
        }
        return "all";
    }

    private String normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
