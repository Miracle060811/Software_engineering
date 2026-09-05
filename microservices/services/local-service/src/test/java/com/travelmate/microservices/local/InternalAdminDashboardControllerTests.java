package com.travelmate.microservices.local;

import com.travelmate.entity.AttractionOrder;
import com.travelmate.entity.HotelOrder;
import com.travelmate.entity.TourOrder;
import com.travelmate.mapper.AttractionOrderMapper;
import com.travelmate.mapper.HotelOrderMapper;
import com.travelmate.mapper.TourOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAdminDashboardControllerTests {
    private HotelOrderMapper hotelOrders;
    private AttractionOrderMapper attractionOrders;
    private TourOrderMapper tourOrders;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        hotelOrders = mock(HotelOrderMapper.class);
        attractionOrders = mock(AttractionOrderMapper.class);
        tourOrders = mock(TourOrderMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new InternalAdminDashboardController(hotelOrders, attractionOrders, tourOrders, "token"))
                .build();
    }

    @Test
    void returnsAllLocalOrderCategoriesInNewestFirstOrder() throws Exception {
        HotelOrder hotel = new HotelOrder();
        hotel.setAmount(new BigDecimal("688")); hotel.setStatus(1);
        hotel.setHotelName("西湖酒店"); hotel.setCreateTime(LocalDateTime.parse("2026-09-03T08:00:00"));
        AttractionOrder attraction = new AttractionOrder();
        attraction.setAmount(new BigDecimal("120")); attraction.setStatus(2);
        attraction.setCity("杭州"); attraction.setCreateTime(LocalDateTime.parse("2026-09-05T08:00:00"));
        TourOrder tour = new TourOrder();
        tour.setAmount(new BigDecimal("999")); tour.setStatus(1);
        tour.setProductName("杭州周边游"); tour.setCreateTime(LocalDateTime.parse("2026-09-04T08:00:00"));
        when(hotelOrders.selectList(any())).thenReturn(List.of(hotel));
        when(attractionOrders.selectList(any())).thenReturn(List.of(attraction));
        when(tourOrders.selectList(any())).thenReturn(List.of(tour));

        mvc.perform(get("/internal/local/admin/dashboard-orders").header("X-Internal-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("attraction"))
                .andExpect(jsonPath("$[0].destination").value("杭州"))
                .andExpect(jsonPath("$[1].category").value("tour"))
                .andExpect(jsonPath("$[2].category").value("hotel"));
    }

    @Test
    void rejectsInvalidOrMissingInternalToken() throws Exception {
        mvc.perform(get("/internal/local/admin/dashboard-orders").header("X-Internal-Token", "wrong"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/local/admin/dashboard-orders"))
                .andExpect(status().isBadRequest());
    }
}
