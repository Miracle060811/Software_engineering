package com.travelmate.microservices.local;

import com.travelmate.entity.Coupon;
import com.travelmate.entity.UserCoupon;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.UserCouponMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalAdminCouponControllerTests {
    private CouponMapper coupons;
    private UserCouponMapper userCoupons;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        coupons = mock(CouponMapper.class); userCoupons = mock(UserCouponMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new InternalAdminCouponController(coupons, userCoupons, "token")).build();
    }

    @Test void couponCrudAndClaimsWork() throws Exception {
        Coupon coupon = new Coupon(); coupon.setId(3L); coupon.setStatus(0); coupon.setStock(10);
        UserCoupon claim = new UserCoupon(); claim.setId(8L); claim.setUserId(9L); claim.setCouponId(3L); claim.setStatus(0);
        when(coupons.selectList(any())).thenReturn(List.of(coupon));
        when(coupons.selectById(3L)).thenReturn(coupon);
        when(coupons.updateById(any(Coupon.class))).thenReturn(1);
        when(coupons.deleteById(3L)).thenReturn(1);
        when(userCoupons.selectList(any())).thenReturn(List.of(claim));
        String body="{\"name\":\"演示券\",\"category\":\"FLIGHT\",\"discountType\":0,\"discountValue\":20,\"minAmount\":100,\"expireDate\":\"2030-01-01T00:00:00\",\"stock\":10}";
        mvc.perform(get("/internal/local/admin/coupons").header("X-Internal-Token","token")).andExpect(status().isOk());
        mvc.perform(post("/internal/local/admin/coupons").header("X-Internal-Token","token").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category").value("flight"));
        mvc.perform(put("/internal/local/admin/coupons/3").header("X-Internal-Token","token").contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(get("/internal/local/admin/coupons/3/claims").header("X-Internal-Token","token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].userId").value(9));
        when(userCoupons.selectCount(any())).thenReturn(0L);
        mvc.perform(delete("/internal/local/admin/coupons/3").header("X-Internal-Token","token")).andExpect(status().isOk());
        verify(coupons).deleteById(3L);
    }

    @Test void claimedCouponIsDisabledInsteadOfDeleted() throws Exception {
        Coupon coupon = new Coupon(); coupon.setId(3L); coupon.setStatus(0); coupon.setStock(10);
        when(userCoupons.selectCount(any())).thenReturn(1L);
        when(coupons.selectById(3L)).thenReturn(coupon);
        when(coupons.updateById(coupon)).thenReturn(1);
        mvc.perform(delete("/internal/local/admin/coupons/3").header("X-Internal-Token","token")).andExpect(status().isOk());
        verify(coupons, never()).deleteById(3L);
        org.junit.jupiter.api.Assertions.assertEquals(1, coupon.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, coupon.getStock());
    }

    @Test void rejectsInvalidTokenAndInvalidCoupon() throws Exception {
        mvc.perform(get("/internal/local/admin/coupons").header("X-Internal-Token","wrong")).andExpect(status().isForbidden());
        mvc.perform(post("/internal/local/admin/coupons").header("X-Internal-Token","token")
                .contentType("application/json").content("{\"name\":\"坏券\",\"discountType\":0,\"discountValue\":-1,\"minAmount\":0,\"stock\":1}"))
                .andExpect(status().isBadRequest());
    }
}
