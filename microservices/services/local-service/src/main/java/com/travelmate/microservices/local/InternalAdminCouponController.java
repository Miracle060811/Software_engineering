package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Coupon;
import com.travelmate.entity.UserCoupon;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.UserCouponMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/internal/local/admin/coupons")
public class InternalAdminCouponController {
    private final CouponMapper coupons;
    private final UserCouponMapper userCoupons;
    private final String token;

    public InternalAdminCouponController(CouponMapper coupons, UserCouponMapper userCoupons,
                                         @Value("${app.internal-service-token}") String token) {
        this.coupons = coupons; this.userCoupons = userCoupons; this.token = token;
    }

    @GetMapping
    public List<Coupon> list(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return coupons.selectList(new LambdaQueryWrapper<Coupon>()
                .orderByDesc(Coupon::getCreateTime).orderByDesc(Coupon::getId));
    }

    @PostMapping
    public Coupon add(@RequestBody Coupon coupon, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); normalizeAndValidate(coupon); coupon.setId(null);
        if (coupon.getCreateTime() == null) coupon.setCreateTime(LocalDateTime.now());
        coupons.insert(coupon); return coupon;
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody Coupon coupon,
                       @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied); normalizeAndValidate(coupon); coupon.setId(id);
        if (coupons.updateById(coupon) == 0) notFound();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        if (userCoupons.selectCount(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, id)) > 0) {
            Coupon coupon = coupons.selectById(id);
            if (coupon == null) notFound();
            coupon.setStatus(1); coupon.setStock(0);
            if (coupons.updateById(coupon) == 0) notFound();
        } else if (coupons.deleteById(id) == 0) notFound();
    }

    @GetMapping("/{id}/claims")
    public List<Map<String, Object>> claims(@PathVariable Long id,
                                            @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return userCoupons.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, id).orderByDesc(UserCoupon::getReceivedTime)).stream().map(claim -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", claim.getId()); row.put("userId", claim.getUserId()); row.put("status", claim.getStatus());
            row.put("receivedTime", claim.getReceivedTime()); row.put("usedTime", claim.getUsedTime());
            return row;
        }).toList();
    }

    private void normalizeAndValidate(Coupon coupon) {
        text(coupon.getName(), "优惠券名称");
        String category = coupon.getCategory() == null ? "" : coupon.getCategory().trim().toLowerCase(Locale.ROOT);
        coupon.setCategory(List.of("flight", "train", "hotel").contains(category) ? category : "all");
        if (coupon.getDiscountType() == null || (coupon.getDiscountType() != 0 && coupon.getDiscountType() != 1)) bad("优惠类型必须为满减或折扣");
        nonNegative(coupon.getDiscountValue(), "优惠值"); nonNegative(coupon.getMinAmount(), "最低消费");
        if (coupon.getStock() == null || coupon.getStock() < 0) bad("库存不能为负数");
        if (coupon.getExpireDate() == null) bad("有效期不能为空");
        if (coupon.getDiscountType() == 1 && coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) bad("折扣比例必须大于 0");
        if (coupon.getStatus() == null) coupon.setStatus(0);
    }

    private void text(String value, String field) { if (value == null || value.isBlank()) bad(field + "不能为空"); }
    private void nonNegative(BigDecimal value, String field) { if (value == null || value.signum() < 0) bad(field + "不能为负数"); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void notFound() { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "优惠券不存在"); }
    private void verify(String supplied) { if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务令牌无效"); }
}
