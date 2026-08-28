package com.travelmate;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.travelmate.dto.HotelOrderCreateDTO;
import com.travelmate.entity.Hotel;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.CouponService;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.NotificationCenterService;
import com.travelmate.service.StockPreDeductResult;
import com.travelmate.service.impl.HotelOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCase05And06HotelWorkflowTests {

    private HotelOrderServiceImpl service;
    private HotelOrderMapper orderMapper;
    private HotelMapper hotelMapper;
    private HotelRoomMapper roomMapper;
    private HotelRoomStockService stockService;
    private CouponService couponService;
    private NotificationCenterService notificationService;

    @BeforeEach
    void setUp() {
        service = new HotelOrderServiceImpl();
        orderMapper = mock(HotelOrderMapper.class);
        hotelMapper = mock(HotelMapper.class);
        roomMapper = mock(HotelRoomMapper.class);
        stockService = mock(HotelRoomStockService.class);
        couponService = mock(CouponService.class);
        notificationService = mock(NotificationCenterService.class);
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        ReflectionTestUtils.setField(service, "hotelMapper", hotelMapper);
        ReflectionTestUtils.setField(service, "hotelRoomMapper", roomMapper);
        ReflectionTestUtils.setField(service, "hotelRoomStockService", stockService);
        ReflectionTestUtils.setField(service, "couponService", couponService);
        ReflectionTestUtils.setField(service, "notificationCenterService", notificationService);
    }

    @Test
    void intTc105CreatesTwoNightTwoRoomOrderWithExactAmount() {
        when(hotelMapper.selectById(11L)).thenReturn(hotel());
        when(roomMapper.selectById(12L)).thenReturn(room());
        when(stockService.preDeductRoom(12L, 8, 2)).thenReturn(StockPreDeductResult.DEDUCTED_IN_REDIS);
        when(roomMapper.deductRoom(12L, 2)).thenReturn(1);
        when(couponService.useCoupon(7L, null, new BigDecimal("800.00"), "hotel"))
                .thenReturn(new BigDecimal("800.00"));
        when(orderMapper.insert(any(HotelOrder.class))).thenReturn(1);

        String orderNo = service.createOrder(7L, dto(2));

        assertThat(orderNo).startsWith("HT");
        ArgumentCaptor<HotelOrder> captor = ArgumentCaptor.forClass(HotelOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getNights()).isEqualTo(2);
        assertThat(captor.getValue().getRoomCount()).isEqualTo(2);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("800.00");
        verify(notificationService).createNotification(eq(7L), eq("hotel_order"), eq("酒店订单已创建"), any(), any());
    }

    @Test
    void unitTc105RejectsInvalidDateBeforeReadingInventory() {
        HotelOrderCreateDTO dto = dto(1);
        dto.setCheckOutDate(dto.getCheckInDate());

        assertThatThrownBy(() -> service.createOrder(7L, dto)).hasMessage("入住/退房日期不合法");
        verify(hotelMapper, never()).selectById(any());
        verify(roomMapper, never()).deductRoom(any(), any());
    }

    @Test
    void unitTc105RollsBackRedisReservationWhenDatabaseStockLosesRace() {
        when(hotelMapper.selectById(11L)).thenReturn(hotel());
        when(roomMapper.selectById(12L)).thenReturn(room());
        when(stockService.preDeductRoom(12L, 8, 2)).thenReturn(StockPreDeductResult.DEDUCTED_IN_REDIS);
        when(roomMapper.deductRoom(12L, 2)).thenReturn(0);

        assertThatThrownBy(() -> service.createOrder(7L, dto(2))).hasMessageContaining("暂无可用房间");
        verify(stockService).rollbackPreDeduct(12L, 2);
        verify(orderMapper, never()).insert(any(HotelOrder.class));
    }

    @Test
    void intTc106CancelsPendingOrderAndReturnsExactRoomCountOnce() {
        HotelOrder order = order(0, 3);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markCancelledFromPending(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.cancelOrder(7L, order.getOrderNo())).isTrue();
        verify(roomMapper).returnRoom(12L, 3);
        verify(stockService).syncWithDatabase(12L);
    }

    @Test
    void unitTc106ConcurrentCancellationDoesNotReturnInventoryTwice() {
        HotelOrder order = order(0, 3);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markCancelledFromPending(7L, order.getOrderNo())).thenReturn(0);

        assertThatThrownBy(() -> service.cancelOrder(7L, order.getOrderNo()))
                .hasMessage("订单状态已变化，请刷新后重试");
        verify(roomMapper, never()).returnRoom(any(), any());
        verify(stockService, never()).syncWithDatabase(any());
    }

    @Test
    void intTc106PaidOrderCanRequestRefundButCannotBeCancelledDirectly() {
        HotelOrder order = order(1, 1);
        when(orderMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(order);
        when(orderMapper.markRefundRequested(7L, order.getOrderNo())).thenReturn(1);

        assertThat(service.requestRefund(7L, order.getOrderNo())).isTrue();
        verify(orderMapper).markRefundRequested(7L, order.getOrderNo());
        assertThatThrownBy(() -> service.cancelOrder(7L, order.getOrderNo()))
                .hasMessage("只有待支付的订单才能取消");
    }

    private HotelOrderCreateDTO dto(int roomCount) {
        HotelOrderCreateDTO dto = new HotelOrderCreateDTO();
        dto.setHotelId(11L);
        dto.setRoomId(12L);
        dto.setRoomCount(roomCount);
        dto.setCheckInDate(LocalDate.now().plusDays(1));
        dto.setCheckOutDate(LocalDate.now().plusDays(3));
        dto.setGuestName("测试住客");
        dto.setGuestPhone("13800138000");
        return dto;
    }

    private Hotel hotel() {
        Hotel hotel = new Hotel();
        hotel.setId(11L);
        hotel.setName("测试酒店");
        hotel.setStatus(1);
        return hotel;
    }

    private HotelRoom room() {
        HotelRoom room = new HotelRoom();
        room.setId(12L);
        room.setHotelId(11L);
        room.setRoomType("标准间");
        room.setPrice(new BigDecimal("200.00"));
        room.setAvailableRooms(8);
        room.setStatus(1);
        return room;
    }

    private HotelOrder order(int status, int roomCount) {
        HotelOrder order = new HotelOrder();
        order.setOrderNo("HT-TEST-001");
        order.setUserId(7L);
        order.setRoomId(12L);
        order.setRoomCount(roomCount);
        order.setStatus(status);
        return order;
    }
}
