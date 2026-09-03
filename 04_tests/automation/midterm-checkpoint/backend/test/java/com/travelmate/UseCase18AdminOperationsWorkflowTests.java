package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.controller.AdminController;
import com.travelmate.entity.Flight;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase18AdminOperationsWorkflowTests {
    private AdminController controller;
    private UserMapper userMapper;
    private FlightMapper flightMapper;
    private TrafficOrderMapper trafficOrderMapper;
    private User admin;

    @BeforeEach
    void setUp() {
        controller = new AdminController();
        userMapper = mock(UserMapper.class);
        flightMapper = mock(FlightMapper.class);
        trafficOrderMapper = mock(TrafficOrderMapper.class);
        admin = new User();
        admin.setId(9L);
        admin.setUsername("admin-test");
        admin.setRole(1);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "flightMapper", flightMapper);
        ReflectionTestUtils.setField(controller, "trafficOrderMapper", trafficOrderMapper);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-test", null, "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void intTc118CreatesValidatedFlightResource() {
        Flight flight = validFlight();
        Result<Flight> result = controller.addFlight(flight);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(flight);
        verify(flightMapper).insert(flight);
    }

    @Test
    void unitTc118RejectsInvalidInventoryBeforeWriting() {
        Flight flight = validFlight();
        flight.setAvailableSeats(101);
        assertThatThrownBy(() -> controller.addFlight(flight))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能大于总座位数");
        verify(flightMapper, never()).insert(any(Flight.class));
    }

    @Test
    void intTc118ProtectsResourcesReferencedByOrders() {
        when(trafficOrderMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        Result<Void> result = controller.deleteFlight(88L);
        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("已有订单");
        verify(flightMapper, never()).deleteById(88L);
    }

    @Test
    void unitTc118RejectsNonAdminAndSelfDisable() {
        User ordinary = new User();
        ordinary.setId(42L);
        ordinary.setRole(0);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(ordinary);
        assertThatThrownBy(controller::listFlights)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无管理员权限");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        assertThatThrownBy(() -> controller.disableUser(admin.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能禁用当前管理员");
    }

    private Flight validFlight() {
        Flight flight = new Flight();
        flight.setFlightNo("CI1801");
        flight.setAirline("CI 航空");
        flight.setDepartureCity("北京");
        flight.setArrivalCity("上海");
        flight.setDepartureTime(LocalDateTime.of(2026, 9, 1, 8, 0));
        flight.setArrivalTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        flight.setEconomyPrice(new BigDecimal("680"));
        flight.setBusinessPrice(new BigDecimal("1880"));
        flight.setTotalSeats(100);
        flight.setAvailableSeats(80);
        flight.setStatus(1);
        return flight;
    }
}
