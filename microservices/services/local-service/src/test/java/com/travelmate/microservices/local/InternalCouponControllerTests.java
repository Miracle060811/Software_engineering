package com.travelmate.microservices.local;

import com.travelmate.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalCouponControllerTests {

    @Test
    void validInternalCouponRedemptionReturnsDiscount() throws Exception {
        CouponService couponService = mock(CouponService.class);
        when(couponService.useCoupon(7L, 5L, new BigDecimal("100.00"), "flight"))
                .thenReturn(new BigDecimal("20.00"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalCouponController(couponService, "service-token")).build();

        mvc.perform(post("/internal/local/coupons/redeem")
                        .header("X-Internal-Token", "service-token")
                        .contentType("application/json")
                        .content("{\"userId\":7,\"userCouponId\":5,\"amount\":100.00,\"businessType\":\"flight\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(20.00));
    }

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

    @Test
    void invalidInternalCredentialIsRejectedOverHttp() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalCouponController(mock(CouponService.class), "service-token")).build();
        mvc.perform(post("/internal/local/coupons/redeem")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedInternalCouponRequestIsRejected() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalCouponController(mock(CouponService.class), "service-token")).build();
        mvc.perform(post("/internal/local/coupons/redeem")
                        .header("X-Internal-Token", "service-token"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/internal/local/coupons/redeem")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
