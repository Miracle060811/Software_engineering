package com.travelmate.microservices.traffic;

import com.travelmate.entity.Flight;
import com.travelmate.entity.Train;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrainMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import com.travelmate.integration.NotificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminTrafficControllerTests {
    private FlightMapper flightMapper;
    private TrainMapper trainMapper;
    private TrafficOrderMapper orderMapper;
    private NotificationGateway notificationGateway;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flightMapper = mock(FlightMapper.class);
        trainMapper = mock(TrainMapper.class);
        orderMapper = mock(TrafficOrderMapper.class);
        notificationGateway = mock(NotificationGateway.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalAdminTrafficController(flightMapper, trainMapper, orderMapper, notificationGateway, "shared-token")).build();
    }

    @Test
    void adminTrafficEndpointsReturnFlightsOrdersAndCount() throws Exception {
        Flight flight = new Flight(); flight.setId(1L);
        TrafficOrder order = new TrafficOrder(); order.setOrderNo("O-1"); order.setOrderType(1); order.setTicketId(2L);
        Train train = new Train(); train.setId(2L); train.setDepartureStation("杭州东"); train.setArrivalStation("上海虹桥");
        when(flightMapper.selectList(any())).thenReturn(List.of(flight));
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(trainMapper.selectById(2L)).thenReturn(train);
        when(orderMapper.selectCount(null)).thenReturn(1L);

        mockMvc.perform(get("/internal/traffic/admin/flights").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(get("/internal/traffic/admin/orders").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].orderNo").value("O-1"))
                .andExpect(jsonPath("$[0].departureStation").value("杭州东"))
                .andExpect(jsonPath("$[0].arrivalStation").value("上海虹桥"));
        mockMvc.perform(get("/internal/traffic/admin/order-count").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
    }

    @Test
    void adminTrafficEndpointsRejectInvalidInternalToken() throws Exception {
        for (String path : List.of("/internal/traffic/admin/flights", "/internal/traffic/admin/orders",
                "/internal/traffic/admin/order-count")) {
            mockMvc.perform(get(path).header("X-Internal-Token", "wrong-token")).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminTrafficEndpointsRequireInternalTokenHeader() throws Exception {
        for (String path : List.of("/internal/traffic/admin/flights", "/internal/traffic/admin/orders",
                "/internal/traffic/admin/order-count")) {
            mockMvc.perform(get(path)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void adminCanCreateUpdateAndDeleteFlightsAndTrains() throws Exception {
        when(flightMapper.updateById(any(Flight.class))).thenReturn(1);
        when(flightMapper.deleteById(1L)).thenReturn(1);
        when(trainMapper.updateById(any(Train.class))).thenReturn(1);
        when(trainMapper.deleteById(2L)).thenReturn(1);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        String flight = "{\"flightNo\":\"CA1\",\"airline\":\"国航\",\"departureCity\":\"北京\",\"arrivalCity\":\"上海\",\"departureTime\":\"2026-09-05T08:00:00\",\"arrivalTime\":\"2026-09-05T10:00:00\",\"economyPrice\":500,\"businessPrice\":1000,\"totalSeats\":100,\"availableSeats\":80}";
        String train = "{\"trainNo\":\"G1\",\"trainType\":\"G\",\"departureStation\":\"北京南\",\"arrivalStation\":\"上海虹桥\",\"departureTime\":\"2026-09-05T08:00:00\",\"arrivalTime\":\"2026-09-05T12:00:00\",\"firstClassPrice\":800,\"secondClassPrice\":500,\"firstClassSeats\":20,\"secondClassSeats\":100}";

        mockMvc.perform(post("/internal/traffic/admin/flights").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content(flight)).andExpect(status().isOk());
        mockMvc.perform(put("/internal/traffic/admin/flights/1").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content(flight)).andExpect(status().isOk());
        mockMvc.perform(delete("/internal/traffic/admin/flights/1").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/traffic/admin/trains").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content(train)).andExpect(status().isOk());
        mockMvc.perform(put("/internal/traffic/admin/trains/2").header("X-Internal-Token", "shared-token")
                        .contentType("application/json").content(train)).andExpect(status().isOk());
        mockMvc.perform(delete("/internal/traffic/admin/trains/2").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk());

        verify(flightMapper).insert(any(Flight.class));
        verify(trainMapper).insert(any(Train.class));
    }

    @Test
    void adminCannotDeleteTransportReferencedByAnOrder() throws Exception {
        when(orderMapper.selectCount(any())).thenReturn(1L);
        mockMvc.perform(delete("/internal/traffic/admin/flights/1").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/internal/traffic/admin/trains/2").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isConflict());
    }

    @Test void adminCanApproveRejectRefundAndCompleteTicket() throws Exception {
        TrafficOrder order = new TrafficOrder(); order.setOrderNo("T-1"); order.setUserId(7L);
        order.setOrderType(1); order.setTicketId(2L); order.setTicketCount(2); order.setSeatType("FirstClass");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.markRefundApproved("T-1")).thenReturn(1);
        order.setStatus(5);
        mockMvc.perform(post("/internal/traffic/admin/orders/T-1/refund/approve").header("X-Internal-Token","shared-token"))
                .andExpect(status().isOk());
        verify(trainMapper).returnFirstClassSeat(2L, 2);

        when(orderMapper.markRefundRejected("T-1")).thenReturn(1);
        mockMvc.perform(post("/internal/traffic/admin/orders/T-1/refund/reject").header("X-Internal-Token","shared-token"))
                .andExpect(status().isOk());

        order.setStatus(1); when(orderMapper.markTicketed("T-1")).thenReturn(1);
        mockMvc.perform(post("/internal/traffic/admin/orders/T-1/ticket/complete").header("X-Internal-Token","shared-token"))
                .andExpect(status().isOk());
        verify(notificationGateway, org.mockito.Mockito.times(3)).publish(any(), any(), any(), any(), any());
    }

    @Test void repeatedRefundApprovalDoesNotReturnStockAgain() throws Exception {
        TrafficOrder order = new TrafficOrder(); order.setOrderNo("T-2"); order.setStatus(4);
        when(orderMapper.selectOne(any())).thenReturn(order);
        mockMvc.perform(post("/internal/traffic/admin/orders/T-2/refund/approve").header("X-Internal-Token","shared-token"))
                .andExpect(status().isOk());
        verify(flightMapper, org.mockito.Mockito.never()).returnSeat(any(), any());
        verify(trainMapper, org.mockito.Mockito.never()).returnFirstClassSeat(any(), any());
    }
}
