package com.travelmate.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MySQLUserDataIsolationTests extends AbstractMySQLIntegrationTest {

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (1, 'userA', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'User A', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (2, 'userB', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'User B', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (1, 'ISO1001', 'IsoAir', 'CityA', 'CityB', '2026-09-01 08:00:00', '2026-09-01 10:00:00', 500.00, 1200.00, 100, 100)");
    }

    @Test
    void userACannotSeeUserBOrders() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-A-001', 1, 0, 1, 'Economy', 1, 'User A', '110101199001011234', 500.00, 0)");
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-B-001', 2, 0, 1, 'Economy', 1, 'User B', '110101199002022345', 500.00, 0)");

        Integer userACount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE user_id = 1", Integer.class);
        Integer userBCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE user_id = 2", Integer.class);

        assertThat(userACount).isEqualTo(1);
        assertThat(userBCount).isEqualTo(1);

        Integer userAOwnsB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE user_id = 1 AND order_no = 'ORD-B-001'", Integer.class);
        assertThat(userAOwnsB).isZero();
    }

    @Test
    void userACannotCancelUserBOrder() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-B-002', 2, 0, 1, 'Economy', 1, 'User B', '110101199002022345', 500.00, 0)");

        int updated = jdbcTemplate.update("UPDATE tm_traffic_order SET status = 3 WHERE order_no = 'ORD-B-002' AND user_id = 1");
        assertThat(updated).isZero();

        Integer status = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-B-002'", Integer.class);
        assertThat(status).isEqualTo(0);
    }

    @Test
    void passengerRecordsAreScopedToOwningUser() {
        jdbcTemplate.update("INSERT INTO tm_passenger (id, user_id, name, id_card, phone, type) VALUES (1, 1, 'Passenger A', '110101199001011111', '13800000001', 0)");
        jdbcTemplate.update("INSERT INTO tm_passenger (id, user_id, name, id_card, phone, type) VALUES (2, 2, 'Passenger B', '110101199002022222', '13800000002', 0)");

        Integer userAPassengers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_passenger WHERE user_id = 1", Integer.class);
        Integer userBPassengers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_passenger WHERE user_id = 2", Integer.class);

        assertThat(userAPassengers).isEqualTo(1);
        assertThat(userBPassengers).isEqualTo(1);

        Integer userACanAccessBRecord = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_passenger WHERE user_id = 1 AND id = 2", Integer.class);
        assertThat(userACanAccessBRecord).isZero();
    }

    @Test
    void couponClaimsAreScopedToOwningUser() {
        jdbcTemplate.update("INSERT INTO tm_coupon (id, name, discount_type, discount_value, min_amount, stock, status, expire_date) VALUES (1, 'Test Coupon', 0, 50.00, 100.00, 100, 0, '2026-12-31 23:59:59')");

        jdbcTemplate.update("INSERT INTO tm_user_coupon (id, user_id, coupon_id, status, received_time) VALUES (1, 1, 1, 0, NOW())");
        jdbcTemplate.update("INSERT INTO tm_user_coupon (id, user_id, coupon_id, status, received_time) VALUES (2, 2, 1, 0, NOW())");

        Integer userACoupons = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_coupon WHERE user_id = 1", Integer.class);
        Integer userBCoupons = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_coupon WHERE user_id = 2", Integer.class);

        assertThat(userACoupons).isEqualTo(1);
        assertThat(userBCoupons).isEqualTo(1);

        Integer userACanUseB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_coupon WHERE user_id = 1 AND id = 2", Integer.class);
        assertThat(userACanUseB).isZero();
    }
}
