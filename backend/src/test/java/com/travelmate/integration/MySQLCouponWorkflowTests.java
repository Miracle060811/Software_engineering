package com.travelmate.integration;

import com.travelmate.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySQLCouponWorkflowTests extends AbstractMySQLIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Override
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM tm_user_coupon WHERE user_id = 110 OR coupon_id = 110");
        jdbcTemplate.update("DELETE FROM tm_coupon WHERE id = 110");
        jdbcTemplate.update("DELETE FROM tm_user WHERE id = 110");
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (110, 'inttc110', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'INT-TC-110', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_coupon (id, name, description, category, discount_type, discount_value, min_amount, expire_date, stock, status) VALUES (110, 'INT-TC-110 机票券', '领取到核销集成测试', 'flight', 0, 30.00, 500.00, DATE_ADD(NOW(), INTERVAL 1 DAY), 2, 0)");
    }

    @Test
    void intTc110ClaimsThenUsesCouponOnceWithPersistedStockAndStatus() {
        assertThat(couponService.claimCoupon(110L, 110L)).isEqualTo("领取成功");

        Long userCouponId = jdbcTemplate.queryForObject(
                "SELECT id FROM tm_user_coupon WHERE user_id = 110 AND coupon_id = 110",
                Long.class);
        assertThat(userCouponId).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock FROM tm_coupon WHERE id = 110", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM tm_user_coupon WHERE id = ?", Integer.class, userCouponId)).isZero();

        assertThat(couponService.useCoupon(110L, userCouponId, new BigDecimal("680.00"), "flight"))
                .isEqualByComparingTo("650.00");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM tm_user_coupon WHERE id = ?", Integer.class, userCouponId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_time IS NOT NULL FROM tm_user_coupon WHERE id = ?", Boolean.class, userCouponId)).isTrue();
        assertThatThrownBy(() -> couponService.useCoupon(
                110L, userCouponId, new BigDecimal("680.00"), "flight"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("优惠券已使用或已失效");
    }
}
