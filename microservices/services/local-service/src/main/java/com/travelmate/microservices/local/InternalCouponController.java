package com.travelmate.microservices.local;

import com.travelmate.service.CouponService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/local/coupons")
public class InternalCouponController {
    private final CouponService couponService;
    private final String serviceToken;

    public InternalCouponController(CouponService couponService,
                                    @Value("${app.internal-service-token}") String serviceToken) {
        this.couponService = couponService;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/redeem")
    public BigDecimal redeem(@RequestBody CouponRedemption request,
                             @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        return couponService.useCoupon(request.userId(), request.userCouponId(), request.amount(), request.businessType());
    }

    private void verify(String token) {
        if (!serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        }
    }

    public record CouponRedemption(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
    }
}
