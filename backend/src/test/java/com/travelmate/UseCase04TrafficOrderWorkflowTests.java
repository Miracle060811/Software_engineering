package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.impl.TrafficOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase04TrafficOrderWorkflowTests {

    private TrafficOrderServiceImpl service;
    private TrafficOrderMapper orderMapper;
    private FlightMapper flightMapper;
    private TrainMapper trainMapper;
    private NotificationCenterService notificationCenterService;

    @BeforeEach
    void setUp() {
        service = new TrafficOrderServiceImpl();
        orderMapper = mock(TrafficOrderMapper.class);
        flightMapper = mock(FlightMapper.class);
        trainMapper = mock(TrainMapper.class);
        notificationCenterService = mock(NotificationCenterService.class);

        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "flightMapper", flightMapper);
        ReflectionTestUtils.setField(service, "trainMapper", trainMapper);
        ReflectionTestUtils.setField(service, "notificationCenterService", notificationCenterService);
    }

    @Test
    void unitTc104PaysPendingOrderOnceAndNotifiesUser() {
        TrafficOrder order = order(0, 0, "Economy", 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markPaid(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.payOrder(7L, order.getOrderNo())).isTrue();

        verify(orderMapper).markPaid(7L, order.getOrderNo());
        verify(notificationCenterService).createNotification(
                eq(7L), eq("traffic_order"), eq("机票购票成功"),
                eq("订单 T202608270001 支付成功，系统正在为您出票。"),
                eq("/my-orders?tab=traffic"));
    }

    @Test
    void unitTc104RejectsRepeatedPayment() {
        TrafficOrder order = order(1, 0, "Economy", 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);

        assertThatThrownBy(() -> service.payOrder(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无法再次支付");
        verify(orderMapper, never()).markPaid(7L, order.getOrderNo());
    }

    @Test
    void unitTc104RejectsPaymentWhenOrderStateChangesConcurrentlyWithoutNotification() {
        TrafficOrder order = order(0, 0, "Economy", 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markPaid(7L, order.getOrderNo())).thenReturn(0);

        assertThatThrownBy(() -> service.payOrder(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("订单状态已变化，请刷新后重试");
        verify(notificationCenterService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void unitTc104CancelsPendingFlightOrderAndReturnsExactInventory() {
        TrafficOrder order = order(0, 0, "Economy", 2);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markCancelledFromPending(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.cancelOrder(7L, order.getOrderNo())).isTrue();

        verify(orderMapper).markCancelledFromPending(7L, order.getOrderNo());
        verify(flightMapper).returnSeat(99L, 2);
        verify(trainMapper, never()).returnFirstClassSeat(any(), any());
        verify(trainMapper, never()).returnSecondClassSeat(any(), any());
    }

    @Test
    void unitTc104RejectsRepeatedCancellationWithoutReturningInventoryAgain() {
        TrafficOrder order = order(3, 0, "Economy", 2);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);

        assertThatThrownBy(() -> service.cancelOrder(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("非待支付订单");
        verify(orderMapper, never()).markCancelledFromPending(7L, order.getOrderNo());
        verify(flightMapper, never()).returnSeat(any(), any());
    }

    @Test
    void unitTc104RejectsCancellationWhenOrderStateChangesWithoutReturningInventory() {
        TrafficOrder order = order(0, 0, "Economy", 2);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markCancelledFromPending(7L, order.getOrderNo())).thenReturn(0);

        assertThatThrownBy(() -> service.cancelOrder(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("订单状态已变化，请刷新后重试");
        verify(flightMapper, never()).returnSeat(any(), any());
        verify(trainMapper, never()).returnFirstClassSeat(any(), any());
        verify(trainMapper, never()).returnSecondClassSeat(any(), any());
    }

    @Test
    void unitTc104CancelsFirstClassTrainOrderAndReturnsExactInventory() {
        TrafficOrder order = order(0, 1, "FirstClass", 3);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markCancelledFromPending(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.cancelOrder(7L, order.getOrderNo())).isTrue();

        verify(trainMapper).returnFirstClassSeat(99L, 3);
        verify(trainMapper, never()).returnSecondClassSeat(any(), any());
        verify(flightMapper, never()).returnSeat(any(), any());
    }

    @Test
    void unitTc104RequestsRefundForTicketedOrderAndNotifiesUser() {
        TrafficOrder order = order(2, 1, "FirstClass", 1);
        order.setOrderNo("TR202608270001");
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markRefundRequested(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.requestRefund(7L, order.getOrderNo())).isTrue();

        verify(orderMapper).markRefundRequested(7L, order.getOrderNo());
        verify(notificationCenterService).createNotification(
                eq(7L), eq("traffic_order"), eq("退票申请已提交"),
                eq("订单 TR202608270001 已提交退票申请，请等待管理员处理。"),
                eq("/my-orders?tab=traffic"));
    }

    @Test
    void unitTc104RejectsRefundBeforePayment() {
        TrafficOrder order = order(0, 0, "Economy", 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);

        assertThatThrownBy(() -> service.requestRefund(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只有出票中或已出票订单");
        verify(orderMapper, never()).markRefundRequested(7L, order.getOrderNo());
    }

    @Test
    void unitTc104RejectsRefundWhenOrderStateChangesConcurrentlyWithoutNotification() {
        TrafficOrder order = order(2, 1, "SecondClass", 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markRefundRequested(7L, order.getOrderNo())).thenReturn(0);

        assertThatThrownBy(() -> service.requestRefund(7L, order.getOrderNo()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("订单状态已变化，请刷新后重试");
        verify(notificationCenterService, never()).createNotification(any(), any(), any(), any(), any());
    }

    private TrafficOrder order(int status, int orderType, String seatType, int ticketCount) {
        TrafficOrder order = new TrafficOrder();
        order.setOrderNo("T202608270001");
        order.setUserId(7L);
        order.setOrderType(orderType);
        order.setTicketId(99L);
        order.setSeatType(seatType);
        order.setTicketCount(ticketCount);
        order.setStatus(status);
        return order;
    }
}
