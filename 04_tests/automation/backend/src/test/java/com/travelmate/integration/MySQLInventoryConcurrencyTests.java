package com.travelmate.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MySQLInventoryConcurrencyTests extends AbstractMySQLIntegrationTest {

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (1, 'invuser1', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'invuser1', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_user (id, username, password, nickname, role, status) VALUES (2, 'invuser2', '$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'invuser2', 0, 1)");
        jdbcTemplate.update("INSERT INTO tm_flight (id, flight_no, airline, departure_city, arrival_city, departure_time, arrival_time, economy_price, business_price, total_seats, available_seats) VALUES (1, 'INV1001', 'InvAir', 'CityA', 'CityB', '2026-09-01 08:00:00', '2026-09-01 10:00:00', 500.00, 1200.00, 10, 10)");
    }

    @Test
    void atomicInventoryDeductionPreventsOverselling() throws Exception {
        int threads = 20;
        int seatsToDeduct = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int userId = (i % 2) + 1;
            executor.submit(() -> {
                try {
                    int updated = jdbcTemplate.update(
                            "UPDATE tm_flight SET available_seats = available_seats - 1 WHERE id = 1 AND available_seats > 0");
                    if (updated > 0) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Integer remainingSeats = jdbcTemplate.queryForObject("SELECT available_seats FROM tm_flight WHERE id = 1", Integer.class);
        assertThat(successCount.get()).isEqualTo(seatsToDeduct);
        assertThat(remainingSeats).isZero();
    }

    @Test
    void concurrentInventoryReplenishmentMaintainsCorrectCount() throws Exception {
        jdbcTemplate.update("UPDATE tm_flight SET available_seats = 5 WHERE id = 1");

        int replenishThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(replenishThreads);
        CountDownLatch latch = new CountDownLatch(replenishThreads);

        for (int i = 0; i < replenishThreads; i++) {
            executor.submit(() -> {
                try {
                    jdbcTemplate.update("UPDATE tm_flight SET available_seats = available_seats + 1 WHERE id = 1");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Integer remainingSeats = jdbcTemplate.queryForObject("SELECT available_seats FROM tm_flight WHERE id = 1", Integer.class);
        assertThat(remainingSeats).isEqualTo(15);
    }

    @Test
    void concurrentDeductionAndReplenishmentMaintainsConsistency() throws Exception {
        jdbcTemplate.update("UPDATE tm_flight SET available_seats = 10 WHERE id = 1");

        int threads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger deductSuccess = new AtomicInteger(0);
        AtomicInteger replenishCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int op = i % 3;
            executor.submit(() -> {
                try {
                    if (op == 0) {
                        int updated = jdbcTemplate.update(
                                "UPDATE tm_flight SET available_seats = available_seats - 1 WHERE id = 1 AND available_seats > 0");
                        if (updated > 0) deductSuccess.incrementAndGet();
                    } else {
                        jdbcTemplate.update("UPDATE tm_flight SET available_seats = available_seats + 1 WHERE id = 1");
                        replenishCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Integer remainingSeats = jdbcTemplate.queryForObject("SELECT available_seats FROM tm_flight WHERE id = 1", Integer.class);
        int expected = 10 - deductSuccess.get() + replenishCount.get();
        assertThat(remainingSeats).isEqualTo(expected);
        assertThat(remainingSeats).isNotNegative();
    }
}