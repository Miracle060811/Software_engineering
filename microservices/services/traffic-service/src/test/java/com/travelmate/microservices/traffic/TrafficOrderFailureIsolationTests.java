package com.travelmate.microservices.traffic;

import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.entity.Flight;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.integration.CouponGateway;
import com.travelmate.integration.NotificationGateway;
import com.travelmate.integration.PassengerGateway;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.impl.TrafficOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficOrderFailureIsolationTests {
    private final FlightMapper flightMapper = mock(FlightMapper.class);
    private final TrainMapper trainMapper = mock(TrainMapper.class);
    private final TrafficOrderMapper orderMapper = mock(TrafficOrderMapper.class);
    private final PassengerGateway passengerGateway = mock(PassengerGateway.class);
    private final CouponGateway couponGateway = mock(CouponGateway.class);
    private final NotificationGateway notificationGateway = mock(NotificationGateway.class);
    private final TrafficOrderServiceImpl service = new TrafficOrderServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "flightMapper", flightMapper);
        ReflectionTestUtils.setField(service, "trainMapper", trainMapper);
        ReflectionTestUtils.setField(service, "passengerGateway", passengerGateway);
        ReflectionTestUtils.setField(service, "couponGateway", couponGateway);
        ReflectionTestUtils.setField(service, "notificationGateway", notificationGateway);
    }

    @Test
    void identityUnavailableStopsBeforeInventoryDeduction() {
        when(passengerGateway.findOwnedPassenger(9L, 7L)).thenThrow(unavailable("身份服务暂不可用"));

        assertThatThrownBy(() -> service.createFlightOrder(7L, flightOrder()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");

        verify(flightMapper, never()).selectById(any());
        verify(flightMapper, never()).deductSeat(any(), any(Integer.class));
        verify(orderMapper, never()).insert(any(TrafficOrder.class));
    }

    @Test
    void localUnavailableCreatesNoOrderAndDatabaseDeductionIsTransactional() throws Exception {
        when(passengerGateway.findOwnedPassenger(9L, 7L))
                .thenReturn(new PassengerGateway.PassengerSnapshot(9L, "测试旅客", "110101199001011234"));
        Flight flight = new Flight();
        flight.setId(3L);
        flight.setStatus(1);
        flight.setEconomyPrice(new BigDecimal("500.00"));
        when(flightMapper.selectById(3L)).thenReturn(flight);
        when(flightMapper.deductSeat(3L, 1)).thenReturn(1);
        when(couponGateway.redeem(7L, 5L, new BigDecimal("500.00"), "flight"))
                .thenThrow(unavailable("本地生活服务暂不可用"));

        assertThatThrownBy(() -> service.createFlightOrder(7L, flightOrder()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");

        verify(flightMapper).deductSeat(3L, 1);
        verify(orderMapper, never()).insert(any(TrafficOrder.class));
        Method method = TrafficOrderServiceImpl.class
                .getMethod("createFlightOrder", Long.class, FlightOrderCreateDTO.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
    }

    private FlightOrderCreateDTO flightOrder() {
        FlightOrderCreateDTO dto = new FlightOrderCreateDTO();
        dto.setFlightId(3L);
        dto.setPassengerId(9L);
        dto.setUserCouponId(5L);
        dto.setSeatType("Economy");
        dto.setTicketCount(1);
        return dto;
    }

    private ResponseStatusException unavailable(String reason) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }
}
