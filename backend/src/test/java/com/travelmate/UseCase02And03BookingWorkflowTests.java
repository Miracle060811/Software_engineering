package com.travelmate;

import com.travelmate.dto.FlightOrderCreateDTO;
import com.travelmate.dto.TrainOrderCreateDTO;
import com.travelmate.dto.TrainWaitlistCreateDTO;
import com.travelmate.entity.Flight;
import com.travelmate.entity.Passenger;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.entity.Train;
import com.travelmate.entity.TrainWaitlist;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.TrainWaitlistMapper;
import com.travelmate.service.CouponService;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.impl.TrafficOrderServiceImpl;
import com.travelmate.service.impl.TrainWaitlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class UseCase02And03BookingWorkflowTests {

    private TrafficOrderServiceImpl service;
    private TrafficOrderMapper orderMapper;
    private FlightMapper flightMapper;
    private TrainMapper trainMapper;
    private PassengerMapper passengerMapper;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        service = new TrafficOrderServiceImpl();
        orderMapper = mock(TrafficOrderMapper.class);
        flightMapper = mock(FlightMapper.class);
        trainMapper = mock(TrainMapper.class);
        passengerMapper = mock(PassengerMapper.class);
        couponService = mock(CouponService.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "flightMapper", flightMapper);
        ReflectionTestUtils.setField(service, "trainMapper", trainMapper);
        ReflectionTestUtils.setField(service, "passengerMapper", passengerMapper);
        ReflectionTestUtils.setField(service, "couponService", couponService);
        ReflectionTestUtils.setField(service, "notificationCenterService", mock(NotificationCenterService.class));
        when(orderMapper.insert(any(TrafficOrder.class))).thenReturn(1);
    }

    @Test
    void intTc102CreatesFlightOrderForOwnedPassengerWithExactAmountAndInventory() {
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 7L));
        when(flightMapper.selectById(31L)).thenReturn(flight(31L));
        when(flightMapper.deductSeat(31L, 2)).thenReturn(1);
        when(couponService.useCoupon(7L, null, new BigDecimal("1200.00"), "flight"))
                .thenReturn(new BigDecimal("1200.00"));

        String orderNo = service.createFlightOrder(7L, flightDto(31L, 21L, 2));

        assertThat(orderNo).startsWith("T");
        ArgumentCaptor<TrafficOrder> captor = ArgumentCaptor.forClass(TrafficOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getOrderType()).isZero();
        assertThat(captor.getValue().getPassengerName()).isEqualTo("测试旅客");
        assertThat(captor.getValue().getTicketCount()).isEqualTo(2);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("1200.00");
        verify(flightMapper).deductSeat(31L, 2);
    }

    @Test
    void unitTc102RejectsPassengerOwnedByAnotherUserBeforeInventoryMutation() {
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 8L));

        assertThatThrownBy(() -> service.createFlightOrder(7L, flightDto(31L, 21L, 1)))
                .hasMessage("乘车人选错或不存在");
        verify(flightMapper, never()).deductSeat(any(), any());
        verify(orderMapper, never()).insert(any(TrafficOrder.class));
    }

    @Test
    void unitTc102RejectsSoldOutFlightWithoutCreatingOrder() {
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 7L));
        when(flightMapper.selectById(31L)).thenReturn(flight(31L));
        when(flightMapper.deductSeat(31L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.createFlightOrder(7L, flightDto(31L, 21L, 1)))
                .hasMessageContaining("库存不足");
        verify(orderMapper, never()).insert(any(TrafficOrder.class));
    }

    @Test
    void intTc103CreatesFirstClassTrainOrderAndDeductsSelectedSeatOnly() {
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 7L));
        when(trainMapper.selectById(41L)).thenReturn(train(41L));
        when(trainMapper.deductFirstClassSeat(41L, 2)).thenReturn(1);
        when(couponService.useCoupon(7L, null, new BigDecimal("800.00"), "train"))
                .thenReturn(new BigDecimal("800.00"));

        TrainOrderCreateDTO dto = new TrainOrderCreateDTO();
        dto.setTrainId(41L);
        dto.setPassengerId(21L);
        dto.setSeatType("FirstClass");
        dto.setTicketCount(2);
        String orderNo = service.createTrainOrder(7L, dto);

        assertThat(orderNo).startsWith("TR");
        verify(trainMapper).deductFirstClassSeat(41L, 2);
        verify(trainMapper, never()).deductSecondClassSeat(any(), any());
        ArgumentCaptor<TrafficOrder> captor = ArgumentCaptor.forClass(TrafficOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getSeatType()).isEqualTo("FirstClass");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void unitTc103RejectsSoldOutTrainSeatWithoutCreatingOrder() {
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 7L));
        when(trainMapper.selectById(41L)).thenReturn(train(41L));
        when(trainMapper.deductSecondClassSeat(41L, 1)).thenReturn(0);
        TrainOrderCreateDTO dto = new TrainOrderCreateDTO();
        dto.setTrainId(41L);
        dto.setPassengerId(21L);
        dto.setSeatType("SecondClass");
        dto.setTicketCount(1);

        assertThatThrownBy(() -> service.createTrainOrder(7L, dto))
                .hasMessage("所选席位已售罄或车次停运");
        verify(orderMapper, never()).insert(any(TrafficOrder.class));
    }

    @Test
    void intTc103CreatesWaitlistWithPassengerAndTrainSnapshot() {
        TrainWaitlistMapper waitlistMapper = mock(TrainWaitlistMapper.class);
        TrainWaitlistServiceImpl waitlistService = new TrainWaitlistServiceImpl(trainMapper, passengerMapper);
        ReflectionTestUtils.setField(waitlistService, "baseMapper", waitlistMapper);
        when(passengerMapper.selectById(21L)).thenReturn(passenger(21L, 7L));
        when(trainMapper.selectById(41L)).thenReturn(train(41L));
        when(waitlistMapper.insert(any(TrainWaitlist.class))).thenAnswer(invocation -> {
            invocation.<TrainWaitlist>getArgument(0).setId(81L);
            return 1;
        });
        TrainWaitlistCreateDTO dto = new TrainWaitlistCreateDTO();
        dto.setTrainId(41L);
        dto.setPassengerId(21L);
        dto.setSeatType("SecondClass");
        dto.setTicketCount(2);

        assertThat(waitlistService.createWaitlist(7L, dto)).isEqualTo(81L);
        ArgumentCaptor<TrainWaitlist> captor = ArgumentCaptor.forClass(TrainWaitlist.class);
        verify(waitlistMapper).insert(captor.capture());
        assertThat(captor.getValue().getTrainNo()).isEqualTo("G101");
        assertThat(captor.getValue().getPassengerIdCard()).isEqualTo("TEST-ID");
        assertThat(captor.getValue().getStatus()).isZero();
    }

    private FlightOrderCreateDTO flightDto(Long flightId, Long passengerId, int count) {
        FlightOrderCreateDTO dto = new FlightOrderCreateDTO();
        dto.setFlightId(flightId);
        dto.setPassengerId(passengerId);
        dto.setSeatType("Economy");
        dto.setTicketCount(count);
        return dto;
    }

    private Passenger passenger(Long id, Long userId) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setUserId(userId);
        passenger.setName("测试旅客");
        passenger.setIdCard("TEST-ID");
        return passenger;
    }

    private Flight flight(Long id) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setStatus(1);
        flight.setEconomyPrice(new BigDecimal("600.00"));
        flight.setBusinessPrice(new BigDecimal("1200.00"));
        return flight;
    }

    private Train train(Long id) {
        Train train = new Train();
        train.setId(id);
        train.setStatus(1);
        train.setTrainNo("G101");
        train.setDepartureStation("北京南");
        train.setArrivalStation("上海虹桥");
        train.setDepartureTime(LocalDateTime.now().plusDays(1));
        train.setFirstClassPrice(new BigDecimal("400.00"));
        train.setSecondClassPrice(new BigDecimal("200.00"));
        return train;
    }
}
