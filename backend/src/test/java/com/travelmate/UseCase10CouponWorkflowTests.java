package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.entity.Coupon;
import com.travelmate.entity.UserCoupon;
import com.travelmate.mapper.CouponMapper;
import com.travelmate.mapper.UserCouponMapper;
import com.travelmate.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase10CouponWorkflowTests {

    private CouponServiceImpl service;
    private CouponMapper couponMapper;
    private UserCouponMapper userCouponMapper;

    @BeforeEach
    void setUp() {
        service = new CouponServiceImpl();
        couponMapper = mock(CouponMapper.class);
        userCouponMapper = mock(UserCouponMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", couponMapper);
        ReflectionTestUtils.setField(service, "userCouponMapper", userCouponMapper);
    }

    @Test
    void unitTc110ClaimsAvailableCouponAndDeductsStockOnce() {
        Coupon coupon = coupon(11L, "flight", 0, "30.00", "500.00");
        when(couponMapper.selectById(11L)).thenReturn(coupon);
        when(userCouponMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(couponMapper.deductStock(11L)).thenReturn(1);

        assertThat(service.claimCoupon(7L, 11L)).isEqualTo("领取成功");

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getCouponId()).isEqualTo(11L);
        assertThat(captor.getValue().getStatus()).isZero();
        assertThat(captor.getValue().getReceivedTime()).isNotNull();
    }

    @Test
    void unitTc110RejectsClaimWhenAtomicStockDeductionLosesRace() {
        Coupon coupon = coupon(11L, "flight", 0, "30.00", "500.00");
        when(couponMapper.selectById(11L)).thenReturn(coupon);
        when(userCouponMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(couponMapper.deductStock(11L)).thenReturn(0);

        assertThatThrownBy(() -> service.claimCoupon(7L, 11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该优惠券已被领完");
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    void unitTc110AppliesFixedDiscountAndMarksCouponUsedOnce() {
        UserCoupon userCoupon = userCoupon(21L, 7L, 11L, 0);
        Coupon coupon = coupon(11L, "flight", 0, "30.00", "500.00");
        when(userCouponMapper.selectById(21L)).thenReturn(userCoupon);
        when(couponMapper.selectById(11L)).thenReturn(coupon);
        when(userCouponMapper.markUsed(any(), any(), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal amount = service.useCoupon(7L, 21L, new BigDecimal("680.00"), "flight");

        assertThat(amount).isEqualByComparingTo("650.00");
        verify(userCouponMapper).markUsed(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void unitTc110AppliesPercentageDiscountWithMoneyRounding() {
        UserCoupon userCoupon = userCoupon(22L, 7L, 12L, 0);
        Coupon coupon = coupon(12L, "all", 1, "0.85", "0.00");
        when(userCouponMapper.selectById(22L)).thenReturn(userCoupon);
        when(couponMapper.selectById(12L)).thenReturn(coupon);
        when(userCouponMapper.markUsed(any(), any(), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal amount = service.useCoupon(7L, 22L, new BigDecimal("99.99"), "hotel");

        assertThat(amount).isEqualByComparingTo("84.99");
    }

    @Test
    void unitTc110RejectsCouponOwnedByAnotherUser() {
        when(userCouponMapper.selectById(21L)).thenReturn(userCoupon(21L, 8L, 11L, 0));

        assertThatThrownBy(() -> service.useCoupon(7L, 21L, new BigDecimal("680.00"), "flight"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("优惠券不存在或无权使用");
        verify(userCouponMapper, never()).markUsed(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void unitTc110RejectsWrongCategoryAndKeepsCouponUnused() {
        UserCoupon userCoupon = userCoupon(21L, 7L, 11L, 0);
        Coupon coupon = coupon(11L, "hotel", 0, "30.00", "0.00");
        when(userCouponMapper.selectById(21L)).thenReturn(userCoupon);
        when(couponMapper.selectById(11L)).thenReturn(coupon);

        assertThatThrownBy(() -> service.useCoupon(7L, 21L, new BigDecimal("680.00"), "flight"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该优惠券不适用于当前订单类型");
        verify(userCouponMapper, never()).markUsed(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void unitTc110MarksExpiredCouponBeforeRejectingIt() {
        UserCoupon userCoupon = userCoupon(21L, 7L, 11L, 0);
        Coupon coupon = coupon(11L, "flight", 0, "30.00", "0.00");
        coupon.setExpireDate(LocalDateTime.now().minusMinutes(1));
        when(userCouponMapper.selectById(21L)).thenReturn(userCoupon);
        when(couponMapper.selectById(11L)).thenReturn(coupon);

        assertThatThrownBy(() -> service.useCoupon(7L, 21L, new BigDecimal("680.00"), "flight"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("优惠券已过期");
        verify(userCouponMapper).markExpired(21L, 7L);
        verify(userCouponMapper, never()).markUsed(any(), any(), any(LocalDateTime.class));
    }

    private Coupon coupon(Long id, String category, int discountType, String discountValue, String minAmount) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setName("测试优惠券");
        coupon.setDescription("自动化测试");
        coupon.setCategory(category);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(new BigDecimal(discountValue));
        coupon.setMinAmount(new BigDecimal(minAmount));
        coupon.setExpireDate(LocalDateTime.now().plusDays(1));
        coupon.setStock(10);
        coupon.setStatus(0);
        return coupon;
    }

    private UserCoupon userCoupon(Long id, Long userId, Long couponId, int status) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(id);
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(status);
        return userCoupon;
    }
}
