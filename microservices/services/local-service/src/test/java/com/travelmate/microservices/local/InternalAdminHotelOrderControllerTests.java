package com.travelmate.microservices.local;

import com.travelmate.entity.HotelOrder;
import com.travelmate.integration.NotificationGateway;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.HotelRoomStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalAdminHotelOrderControllerTests {
    private HotelOrderMapper orders; private HotelRoomMapper rooms;
    private HotelRoomStockService stock; private NotificationGateway notifications; private MockMvc mvc;

    @BeforeEach void setUp() {
        orders=mock(HotelOrderMapper.class); rooms=mock(HotelRoomMapper.class);
        stock=mock(HotelRoomStockService.class); notifications=mock(NotificationGateway.class);
        mvc=MockMvcBuilders.standaloneSetup(new InternalAdminHotelOrderController(orders,rooms,stock,notifications,"token")).build();
    }

    @Test void approvesAndRejectsHotelRefund() throws Exception {
        HotelOrder order=new HotelOrder(); order.setOrderNo("HT1"); order.setUserId(7L); order.setRoomId(9L); order.setRoomCount(2); order.setStatus(5);
        when(orders.selectOne(any())).thenReturn(order); when(orders.markRefundApproved("HT1")).thenReturn(1);
        mvc.perform(post("/internal/local/admin/orders/HT1/refund/approve").header("X-Internal-Token","token"))
                .andExpect(status().isOk());
        verify(rooms).returnRoom(9L,2); verify(stock).syncWithDatabase(9L);
        when(orders.markRefundRejected("HT1")).thenReturn(1);
        mvc.perform(post("/internal/local/admin/orders/HT1/refund/reject").header("X-Internal-Token","token"))
                .andExpect(status().isOk());
        verify(notifications,times(2)).publish(any(),any(),any(),any(),any());
    }

    @Test void repeatedApprovalDoesNotReturnRoomAgainAndTokenIsRequired() throws Exception {
        HotelOrder order=new HotelOrder(); order.setOrderNo("HT2"); order.setStatus(4);
        when(orders.selectOne(any())).thenReturn(order);
        mvc.perform(post("/internal/local/admin/orders/HT2/refund/approve").header("X-Internal-Token","token"))
                .andExpect(status().isOk());
        verify(rooms,never()).returnRoom(any(),any());
        mvc.perform(post("/internal/local/admin/orders/HT2/refund/approve").header("X-Internal-Token","wrong"))
                .andExpect(status().isForbidden());
    }
}
