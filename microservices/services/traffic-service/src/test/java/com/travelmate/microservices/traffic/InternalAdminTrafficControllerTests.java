package com.travelmate.microservices.traffic;

import com.travelmate.entity.Flight;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.mapper.FlightMapper;
import com.travelmate.mapper.TrafficOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminTrafficControllerTests {
    private FlightMapper flightMapper;
    private TrafficOrderMapper orderMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flightMapper = mock(FlightMapper.class);
        orderMapper = mock(TrafficOrderMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalAdminTrafficController(flightMapper, orderMapper, "shared-token")).build();
    }

    @Test
    void adminTrafficEndpointsReturnFlightsOrdersAndCount() throws Exception {
        Flight flight = new Flight(); flight.setId(1L);
        TrafficOrder order = new TrafficOrder(); order.setOrderNo("O-1");
        when(flightMapper.selectList(any())).thenReturn(List.of(flight));
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(orderMapper.selectCount(null)).thenReturn(1L);

        mockMvc.perform(get("/internal/traffic/admin/flights").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(get("/internal/traffic/admin/orders").header("X-Internal-Token", "shared-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].orderNo").value("O-1"));
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
}
