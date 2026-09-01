package com.travelmate.integration;

import java.math.BigDecimal;

public interface CouponGateway {
    BigDecimal redeem(Long userId, Long userCouponId, BigDecimal amount, String businessType);
}
