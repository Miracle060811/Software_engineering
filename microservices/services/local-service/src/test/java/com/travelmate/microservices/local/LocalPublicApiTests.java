package com.travelmate.microservices.local;

import com.travelmate.common.UserContext;
import com.travelmate.controller.AttractionController;
import com.travelmate.controller.CouponController;
import com.travelmate.controller.HotelController;
import com.travelmate.controller.ReviewController;
import com.travelmate.controller.TourProductController;
import com.travelmate.entity.TourProduct;
import com.travelmate.service.AttractionService;
import com.travelmate.service.CouponService;
import com.travelmate.service.HotelOrderService;
import com.travelmate.service.HotelService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.TourProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void hotelSearchReturnsSuccessfulApiEnvelope() throws Exception {
        HotelController controller = new HotelController();
        HotelService hotelService = mock(HotelService.class);
        ReflectionTestUtils.setField(controller, "hotelService", hotelService);
        ReflectionTestUtils.setField(controller, "hotelOrderService", mock(HotelOrderService.class));
        ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
        when(hotelService.searchHotels(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/hotel/search").param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void unauthenticatedHotelOrderIsRejectedByBusinessEnvelope() throws Exception {
        HotelController controller = new HotelController();
        UserContext userContext = mock(UserContext.class);
        ReflectionTestUtils.setField(controller, "hotelService", mock(HotelService.class));
        ReflectionTestUtils.setField(controller, "hotelOrderService", mock(HotelOrderService.class));
        ReflectionTestUtils.setField(controller, "userContext", userContext);
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/hotel/order/create")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户未登录或Token无效"));
    }

    @Test
    void attractionSearchReturnsSuccessfulApiEnvelope() throws Exception {
        AttractionController controller = new AttractionController();
        AttractionService attractionService = mock(AttractionService.class);
        ReflectionTestUtils.setField(controller, "attractionService", attractionService);
        ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
        when(attractionService.searchAttractions("北京")).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/attraction/search").param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(attractionService).searchAttractions("北京");
    }

    @Test
    void couponListSupportsAnonymousBrowsing() throws Exception {
        CouponController controller = new CouponController();
        CouponService couponService = mock(CouponService.class);
        UserContext userContext = mock(UserContext.class);
        ReflectionTestUtils.setField(controller, "couponService", couponService);
        ReflectionTestUtils.setField(controller, "userContext", userContext);
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);
        when(couponService.listAvailable(isNull())).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/coupon/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
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

    @Test
    void reviewListReturnsSuccessfulApiEnvelope() throws Exception {
        ReviewController controller = new ReviewController();
        ReviewService reviewService = mock(ReviewService.class);
        ReflectionTestUtils.setField(controller, "reviewService", reviewService);
        ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
        when(reviewService.getReviews(9001L, 0)).thenReturn(List.of());

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/review/list")
                        .param("targetId", "9001")
                        .param("targetType", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
