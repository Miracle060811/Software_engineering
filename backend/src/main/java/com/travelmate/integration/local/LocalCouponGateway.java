package com.travelmate.integration.local;

import com.travelmate.integration.CouponGateway;
import com.travelmate.service.CouponService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.integration.mode", havingValue = "local", matchIfMissing = true)
public class LocalCouponGateway implements CouponGateway {
    private final CouponService couponService;

    public LocalCouponGateway(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public BigDecimal redeem(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
        return couponService.useCoupon(userId, userCouponId, amount, businessType);
    }
}
