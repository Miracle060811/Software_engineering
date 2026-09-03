package com.travelmate.integration;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySQLTransactionRollbackTests extends AbstractMySQLIntegrationTest {

    @Test
    void transactionalRollbackPreservesDataConsistencyOnFailure() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (1, 'txuser', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'txuser', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (1, 'TX1001', 'TestAir', 'CityA', 'CityB', '2026-09-01 08:00:00', '2026-09-01 10:00:00', 500.00, 1200.00, 100, 100)");

        try {
            jdbcTemplate.execute("START TRANSACTION");
            jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-TX-ROLLBACK', 1, 0, 1, 'Economy', 1, 'Test User', '110101199001011234', 500.00, 0)");
            jdbcTemplate.update("UPDATE tm_flight SET available_seats = available_seats - 1 WHERE id = 1");
            throw new RuntimeException("simulated failure");
        } catch (RuntimeException e) {
            jdbcTemplate.execute("ROLLBACK");
        }

        Integer orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE order_no = 'ORD-TX-ROLLBACK'", Integer.class);
        Integer availableSeats = jdbcTemplate.queryForObject("SELECT available_seats FROM tm_flight WHERE id = 1", Integer.class);

        assertThat(orderCount).isZero();
        assertThat(availableSeats).isEqualTo(100);
    }

    @Test
    void nestedTransactionRollbackPreservesOuterTransactionState() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (2, 'nestedtx', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'nestedtx', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (2, 'TX2002', 'TestAir', 'CityC', 'CityD', '2026-09-02 08:00:00', '2026-09-02 10:00:00', 600.00, 1500.00, 50, 50)");

        jdbcTemplate.execute("START TRANSACTION");
        jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-TX-OUTER', 2, 0, 2, 'Economy', 1, 'Outer User', '110101199001011111', 600.00, 0)");

        jdbcTemplate.execute("SAVEPOINT sp1");
        try {
            jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-TX-INNER', 2, 0, 2, 'Economy', 1, 'Inner User', '110101199001011112', 600.00, 0)");
            throw new RuntimeException("simulated inner failure");
        } catch (RuntimeException e) {
            jdbcTemplate.execute("ROLLBACK TO SAVEPOINT sp1");
        }

        jdbcTemplate.execute("COMMIT");

        Integer outerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE order_no = 'ORD-TX-OUTER'", Integer.class);
        Integer innerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order WHERE order_no = 'ORD-TX-INNER'", Integer.class);

        assertThat(outerCount).isEqualTo(1);
        assertThat(innerCount).isZero();
    }

    @Test
    void failedTransactionDoesNotLeakDirtyData() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (3, 'dirtytx', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'dirtytx', 0, 1)");

        try {
            jdbcTemplate.execute("START TRANSACTION");
            jdbcTemplate.update("INSERT INTO tm_traffic_order (order_no, user_id, order_type, ticket_id, seat_type, ticket_count, passenger_name, passenger_id_card, amount, status) VALUES ('ORD-DIRTY-001', 3, 0, 1, 'Economy', 1, 'Dirty', '110101199001011113', 100.00, 0)");
            throw new RuntimeException("forced rollback");
        } catch (RuntimeException e) {
            jdbcTemplate.execute("ROLLBACK");
        }

        Integer totalOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_traffic_order", Integer.class);
        assertThat(totalOrders).isZero();
    }
}