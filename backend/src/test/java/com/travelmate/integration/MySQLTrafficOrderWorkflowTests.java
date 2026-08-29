package com.travelmate.integration;

import com.travelmate.service.TrafficOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySQLTrafficOrderWorkflowTests extends AbstractMySQLIntegrationTest {

    @Autowired
    private TrafficOrderService trafficOrderService;

    @Override
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM tm_notification WHERE user_id = 104");
        jdbcTemplate.update("DELETE FROM tm_traffic_order WHERE order_no = 'INT-TC-104-ORDER'");
        jdbcTemplate.update("DELETE FROM tm_flight WHERE id = 104");
        jdbcTemplate.update("DELETE FROM tm_user WHERE id = 104");
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (104, 'inttc104', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'INT-TC-104', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (104, 'IT104', 'Integration Air', '北京', '上海', ?, ?, 680.00, 1280.00, 20, 19)",
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2));
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('INT-TC-104-ORDER', 104, 0, 104, 'Economy', 1, '集成测试用户', '110101199001011234', 680.00, 0)");
    }

    @Test
    void intTc104PaysThenRequestsRefundWithPersistedStateAndNotifications() {
        assertThat(trafficOrderService.payOrder(104L, "INT-TC-104-ORDER")).isTrue();

        assertThat(orderStatus()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pay_time IS NOT NULL FROM tm_traffic_order WHERE order_no = 'INT-TC-104-ORDER'",
                Boolean.class)).isTrue();

        assertThat(trafficOrderService.requestRefund(104L, "INT-TC-104-ORDER")).isTrue();

        assertThat(orderStatus()).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_notification WHERE user_id = 104 AND type = 'traffic_order'",
                Integer.class)).isEqualTo(2);
        assertThatThrownBy(() -> trafficOrderService.requestRefund(104L, "INT-TC-104-ORDER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只有出票中或已出票订单");
    }

    private Integer orderStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM tm_traffic_order WHERE order_no = 'INT-TC-104-ORDER'",
                Integer.class);
    }
}
