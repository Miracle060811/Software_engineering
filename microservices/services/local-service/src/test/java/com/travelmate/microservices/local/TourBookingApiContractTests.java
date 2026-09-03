package com.travelmate.microservices.local;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.controller.TourProductController;
import com.travelmate.dto.TourBookingCreateDTO;
import com.travelmate.entity.TourOrder;
import com.travelmate.service.TourProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TourBookingApiContractTests {
    private TourProductService tourProductService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tourProductService = mock(TourProductService.class);
        userContext = mock(UserContext.class);

        TourProductController controller = new TourProductController();
        ReflectionTestUtils.setField(controller, "tourProductService", tourProductService);
        ReflectionTestUtils.setField(controller, "userContext", userContext);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void tourOrderEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        TourOrder order = order("T-20260904-001", 7L);
        when(tourProductService.listUserOrders(7L)).thenReturn(List.of(order));
        when(tourProductService.getUserOrder(7L, "T-20260904-001")).thenReturn(order);
        when(tourProductService.createBooking(eq(7L), any(TourBookingCreateDTO.class))).thenReturn(order);

        mockMvc.perform(get("/api/tour/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderNo").value("T-20260904-001"));
        mockMvc.perform(get("/api/tour/orders/T-20260904-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(7));
        mockMvc.perform(post("/api/tour/orders")
                        .header("Idempotency-Key", "tour-request-001")
                        .contentType("application/json")
                        .content("{\"productId\":21,\"scheduleId\":31,\"participantCount\":2,"
                                + "\"contactName\":\"测试用户\",\"contactPhone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("T-20260904-001"));

        ArgumentCaptor<TourBookingCreateDTO> request = ArgumentCaptor.forClass(TourBookingCreateDTO.class);
        verify(tourProductService).createBooking(eq(7L), request.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tour-request-001", request.getValue().getIdempotencyKey());
    }

    @Test
    void tourOrderEndpointsEnforceAuthentication() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);

        mockMvc.perform(get("/api/tour/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户未登录或Token无效"));
        mockMvc.perform(get("/api/tour/orders/T-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户未登录或Token无效"));
        mockMvc.perform(post("/api/tour/orders")
                        .contentType("application/json")
                        .content("{\"productId\":21,\"scheduleId\":31,\"participantCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户未登录或Token无效"));

        verify(tourProductService, never()).listUserOrders(any());
        verify(tourProductService, never()).getUserOrder(any(), any());
        verify(tourProductService, never()).createBooking(any(), any());
    }

    @Test
    void tourOrderEndpointsRejectInvalidPayloadAndOutsiderDetail() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(tourProductService.createBooking(eq(7L), any(TourBookingCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("参与人数必须大于0"));
        when(tourProductService.getUserOrder(7L, "OTHER-USERS-ORDER")).thenReturn(null);

        mockMvc.perform(post("/api/tour/orders")
                        .contentType("application/json")
                        .content("{\"productId\":21,\"scheduleId\":31,\"participantCount\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("参与人数必须大于0"));
        mockMvc.perform(post("/api/tour/orders").contentType("application/json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tour/orders/OTHER-USERS-ORDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("订单不存在或无权查看"));
    }

    private TourOrder order(String orderNo, Long userId) {
        TourOrder order = new TourOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(21L);
        order.setScheduleId(31L);
        order.setParticipantCount(2);
        order.setStatus(0);
        return order;
    }
}
