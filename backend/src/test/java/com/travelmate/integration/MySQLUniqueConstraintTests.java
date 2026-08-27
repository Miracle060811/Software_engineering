package com.travelmate.integration;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySQLUniqueConstraintTests extends AbstractMySQLIntegrationTest {

    @Test
    void duplicateUsernameRegistrationIsRejectedAtDatabaseLevel() throws Exception {
        String token = registerAndGetToken("uniqueuser1", "pass123");

        String duplicateBody = "{\"username\":\"uniqueuser1\",\"password\":\"pass456\",\"nickname\":\"dup\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/user/register")
                        .contentType("application/json")
                        .content(duplicateBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(400, 409, 500);
                });
    }

    @Test
    void duplicateOrderNoInsertionIsRejectedAtDatabaseLevel() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (1, 'orderuser', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'orderuser', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (1, 'CA1001', 'Air China', 'Beijing', 'Shanghai', '2026-09-01 08:00:00', '2026-09-01 10:00:00', 800.00, 2000.00, 200, 200)");

        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-DUP-001', 1, 0, 1, 'Economy', 1, 'Zhang San', '110101199001011234', 800.00, 0)");

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-DUP-001', 1, 0, 1, 'Economy', 1, 'Li Si', '110101199002022345', 800.00, 0)"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void duplicateFlightNumberWithSameDepartureTimeIsRejected() {
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (10, 'CA2001', 'Air China', 'Beijing', 'Guangzhou', '2026-09-01 08:00:00', '2026-09-01 11:00:00', 900.00, 2200.00, 200, 200)");

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (11, 'CA2001', 'Air China', 'Beijing', 'Guangzhou', '2026-09-01 08:00:00', '2026-09-01 11:00:00', 900.00, 2200.00, 200, 200)"))
                .isInstanceOf(DuplicateKeyException.class);
    }
}