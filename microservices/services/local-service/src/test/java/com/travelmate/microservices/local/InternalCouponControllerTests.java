package com.travelmate.microservices.local;

import com.travelmate.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalCouponControllerTests {

    @Test
    void invalidInternalCredentialIsRejectedBeforeCouponMutation() {
        CouponService couponService = mock(CouponService.class);
        InternalCouponController controller = new InternalCouponController(couponService, "service-token");
        InternalCouponController.CouponRedemption request = new InternalCouponController.CouponRedemption(
                7L, 5L, new BigDecimal("100.00"), "flight");

        assertThatThrownBy(() -> controller.redeem(request, "wrong-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");

        verify(couponService, never()).useCoupon(any(), any(), any(), any());
    }
}
