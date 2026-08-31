package com.travelmate.microservices.local;

import com.travelmate.controller.TourProductController;
import com.travelmate.entity.TourProduct;
import com.travelmate.service.TourProductService;
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

class LocalPublicApiTests {
    private TourProductService tourProductService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tourProductService = mock(TourProductService.class);
        TourProductController controller = new TourProductController();
        ReflectionTestUtils.setField(controller, "tourProductService", tourProductService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void tourListReturnsLocalServiceContract() throws Exception {
        TourProduct tour = new TourProduct();
        tour.setId(21L);
        when(tourProductService.listByType(0)).thenReturn(List.of(tour));

        mockMvc.perform(get("/api/tour/list").param("type", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(21));
    }

    @Test
    void invalidTourTypeReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/tour/list").param("type", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("游览产品类型必须为0或1"));
    }
}
