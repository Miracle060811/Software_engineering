package com.travelmate.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.NotificationCenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderTimeoutSchedulerTests {

    private OrderTimeoutScheduler scheduler;
    private TrafficOrderMapper trafficOrderMapper;
    private FlightMapper flightMapper;
    private TrainMapper trainMapper;
    private HotelOrderMapper hotelOrderMapper;
    private HotelRoomMapper hotelRoomMapper;
    private HotelRoomStockService hotelRoomStockService;
    private NotificationCenterService notificationCenterService;

    @BeforeEach
    void setUp() {
        scheduler = new OrderTimeoutScheduler();
        trafficOrderMapper = mock(TrafficOrderMapper.class);
        flightMapper = mock(FlightMapper.class);
        trainMapper = mock(TrainMapper.class);
        hotelOrderMapper = mock(HotelOrderMapper.class);
        hotelRoomMapper = mock(HotelRoomMapper.class);
        hotelRoomStockService = mock(HotelRoomStockService.class);
        notificationCenterService = mock(NotificationCenterService.class);

        ReflectionTestUtils.setField(scheduler, "trafficOrderMapper", trafficOrderMapper);
        ReflectionTestUtils.setField(scheduler, "flightMapper", flightMapper);
        ReflectionTestUtils.setField(scheduler, "trainMapper", trainMapper);
        ReflectionTestUtils.setField(scheduler, "hotelOrderMapper", hotelOrderMapper);
        ReflectionTestUtils.setField(scheduler, "hotelRoomMapper", hotelRoomMapper);
        ReflectionTestUtils.setField(scheduler, "hotelRoomStockService", hotelRoomStockService);
        ReflectionTestUtils.setField(scheduler, "notificationCenterService", notificationCenterService);
    }

    @Test
    void cancelsTimedOutTrafficOrderAndReturnsFlightStock() {
        TrafficOrder order = new TrafficOrder();
        order.setUserId(1L);
        order.setOrderNo("ORD-TIMEOUT-01");
        order.setOrderType(0);
        order.setTicketId(10L);
        order.setTicketCount(2);
        order.setCreateTime(LocalDateTime.now().minusMinutes(20));

        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(trafficOrderMapper.markCancelledFromPending(1L, "ORD-TIMEOUT-01")).thenReturn(1);

        scheduler.cancelTimeoutOrders();

        verify(flightMapper).returnSeat(10L, 2);
        verify(notificationCenterService).createNotification(eq(1L), eq("traffic_order"), anyString(), anyString(), anyString());
    }

    @Test
    void cancelsTimedOutTrafficOrderAndReturnsTrainFirstClassStock() {
        TrafficOrder order = new TrafficOrder();
        order.setUserId(2L);
        order.setOrderNo("ORD-TIMEOUT-02");
        order.setOrderType(1);
        order.setSeatType("FirstClass");
        order.setTicketId(20L);
        order.setTicketCount(1);
        order.setCreateTime(LocalDateTime.now().minusMinutes(30));

        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(trafficOrderMapper.markCancelledFromPending(2L, "ORD-TIMEOUT-02")).thenReturn(1);

        scheduler.cancelTimeoutOrders();

        verify(trainMapper).returnFirstClassSeat(20L, 1);
    }

    @Test
    void cancelsTimedOutTrafficOrderAndReturnsTrainSecondClassStock() {
        TrafficOrder order = new TrafficOrder();
        order.setUserId(3L);
        order.setOrderNo("ORD-TIMEOUT-03");
        order.setOrderType(1);
        order.setSeatType("Economy");
        order.setTicketId(30L);
        order.setTicketCount(1);
        order.setCreateTime(LocalDateTime.now().minusMinutes(16));

        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(trafficOrderMapper.markCancelledFromPending(3L, "ORD-TIMEOUT-03")).thenReturn(1);

        scheduler.cancelTimeoutOrders();

        verify(trainMapper).returnSecondClassSeat(30L, 1);
    }

    @Test
    void skipsOrderWhenMarkCancelledReturnsZero() {
        TrafficOrder order = new TrafficOrder();
        order.setUserId(4L);
        order.setOrderNo("ORD-ALREADY-CANCELLED");
        order.setOrderType(0);
        order.setTicketId(40L);
        order.setTicketCount(1);
        order.setCreateTime(LocalDateTime.now().minusMinutes(20));

        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(trafficOrderMapper.markCancelledFromPending(4L, "ORD-ALREADY-CANCELLED")).thenReturn(0);

        scheduler.cancelTimeoutOrders();

        verify(flightMapper, never()).returnSeat(anyLong(), anyInt());
        verify(notificationCenterService, never()).createNotification(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void cancelsTimedOutHotelOrderAndReturnsRoomStock() {
        HotelOrder order = new HotelOrder();
        order.setUserId(6L);
        order.setOrderNo("HOTEL-TIMEOUT-01");
        order.setRoomId(50L);
        order.setRoomCount(2);
        order.setCreateTime(LocalDateTime.now().minusMinutes(25));

        when(hotelOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(hotelOrderMapper.markCancelledFromPending(6L, "HOTEL-TIMEOUT-01")).thenReturn(1);

        scheduler.cancelTimeoutOrders();

        verify(hotelRoomMapper).returnRoom(50L, 2);
        verify(hotelRoomStockService).syncWithDatabase(50L);
        verify(notificationCenterService).createNotification(eq(6L), eq("hotel_order"), anyString(), anyString(), anyString());
    }

    @Test
    void skipsHotelOrderWhenMarkCancelledReturnsZero() {
        HotelOrder order = new HotelOrder();
        order.setUserId(7L);
        order.setOrderNo("HOTEL-ALREADY");
        order.setRoomId(60L);
        order.setRoomCount(1);
        order.setCreateTime(LocalDateTime.now().minusMinutes(20));

        when(hotelOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(hotelOrderMapper.markCancelledFromPending(7L, "HOTEL-ALREADY")).thenReturn(0);

        scheduler.cancelTimeoutOrders();

        verify(hotelRoomMapper, never()).returnRoom(anyLong(), anyInt());
        verify(hotelRoomStockService, never()).syncWithDatabase(anyLong());
    }

    @Test
    void returnsNullTicketCountAsOne() {
        TrafficOrder order = new TrafficOrder();
        order.setUserId(8L);
        order.setOrderNo("ORD-NULL-COUNT");
        order.setOrderType(0);
        order.setTicketId(70L);
        order.setTicketCount(null);
        order.setCreateTime(LocalDateTime.now().minusMinutes(20));

        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
        when(trafficOrderMapper.markCancelledFromPending(8L, "ORD-NULL-COUNT")).thenReturn(1);

        scheduler.cancelTimeoutOrders();

        verify(flightMapper).returnSeat(70L, 1);
    }

    @Test
    void handlesEmptyResultGracefully() {
        when(trafficOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(hotelOrderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        scheduler.cancelTimeoutOrders();

        verify(trafficOrderMapper, never()).markCancelledFromPending(anyLong(), anyString());
        verify(hotelOrderMapper, never()).markCancelledFromPending(anyLong(), anyString());
    }
}