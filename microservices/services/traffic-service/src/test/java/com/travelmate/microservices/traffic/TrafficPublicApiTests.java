package com.travelmate.microservices.traffic;

import com.travelmate.controller.FlightController;
import com.travelmate.entity.Flight;
import com.travelmate.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrafficPublicApiTests {
    private FlightService flightService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flightService = mock(FlightService.class);
        FlightController controller = new FlightController();
        ReflectionTestUtils.setField(controller, "flightService", flightService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void flightSearchReturnsServiceOwnedData() throws Exception {
        Flight flight = new Flight();
        flight.setId(11L);
        flight.setFlightNo("TM1001");
        when(flightService.searchFlights("北京", "上海", null)).thenReturn(List.of(flight));

        mockMvc.perform(get("/api/flight/search")
                        .param("depCity", "北京")
                        .param("arrCity", "上海"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].flightNo").value("TM1001"));
    }

    @Test
    void cancelledFlightIsNotExposedAsBookableDetail() throws Exception {
        Flight flight = new Flight();
        flight.setId(12L);
        flight.setStatus(0);
        when(flightService.getById(12L)).thenReturn(flight);

        mockMvc.perform(get("/api/flight/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
