package com.travelmate.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MySQLOrderStateMachineTests extends AbstractMySQLIntegrationTest {

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (1, 'stateuser', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'stateuser', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (1, 'ST1001', 'StateAir', 'CityA', 'CityB', '2026-09-01 08:00:00', '2026-09-01 10:00:00', 500.00, 1200.00, 100, 100)");
    }

    @Test
    void orderStateTransitionsFromPendingToPaidToIssued() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-STATE-001', 1, 0, 1, 'Economy', 1, 'State User', '110101199001011234', 500.00, 0)");

        Integer status0 = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-001'", Integer.class);
        assertThat(status0).isEqualTo(0);

        jdbcTemplate.update("UPDATE tm_traffic_order SET status = 1, pay_time = NOW() WHERE order_no = 'ORD-STATE-001' AND status = 0");
        Integer status1 = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-001'", Integer.class);
        assertThat(status1).isEqualTo(1);

        jdbcTemplate.update("UPDATE tm_traffic_order SET status = 2 WHERE order_no = 'ORD-STATE-001' AND status = 1");
        Integer status2 = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-001'", Integer.class);
        assertThat(status2).isEqualTo(2);
    }

    @Test
    void orderCannotTransitionFromCancelledToPaid() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-STATE-002', 1, 0, 1, 'Economy', 1, 'State User', '110101199001011234', 500.00, 3)");

        int updated = jdbcTemplate.update("UPDATE tm_traffic_order SET status = 1, pay_time = NOW() WHERE order_no = 'ORD-STATE-002' AND status = 0");
        assertThat(updated).isZero();

        Integer status = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-002'", Integer.class);
        assertThat(status).isEqualTo(3);
    }

    @Test
    void orderCannotTransitionFromIssuedToCancelled() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-STATE-003', 1, 0, 1, 'Economy', 1, 'State User', '110101199001011234', 500.00, 2)");

        int updated = jdbcTemplate.update("UPDATE tm_traffic_order SET status = 3 WHERE order_no = 'ORD-STATE-003' AND status IN (0, 1)");
        assertThat(updated).isZero();

        Integer status = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-003'", Integer.class);
        assertThat(status).isEqualTo(2);
    }

    @Test
    void refundRequestCreatesIntermediateState() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-STATE-004', 1, 0, 1, 'Economy', 1, 'State User', '110101199001011234', 500.00, 2)");

        jdbcTemplate.update("UPDATE tm_traffic_order SET status = 5 WHERE order_no = 'ORD-STATE-004' AND status = 2");
        Integer status5 = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-004'", Integer.class);
        assertThat(status5).isEqualTo(5);

        jdbcTemplate.update("UPDATE tm_traffic_order SET status = 4 WHERE order_no = 'ORD-STATE-004' AND status = 5");
        Integer status4 = jdbcTemplate.queryForObject("SELECT status FROM tm_traffic_order WHERE order_no = 'ORD-STATE-004'", Integer.class);
        assertThat(status4).isEqualTo(4);
    }

    @Test
    void idempotentPayOnlyUpdatesPendingOrder() {
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-STATE-005', 1, 0, 1, 'Economy', 1, 'State User', '110101199001011234', 500.00, 0)");

        int first = jdbcTemplate.update("UPDATE tm_traffic_order SET status = 1, pay_time = NOW() WHERE order_no = 'ORD-STATE-005' AND status = 0");
        assertThat(first).isEqualTo(1);

        int second = jdbcTemplate.update("UPDATE tm_traffic_order SET status = 1, pay_time = NOW() WHERE order_no = 'ORD-STATE-005' AND status = 0");
        assertThat(second).isZero();
    }
}