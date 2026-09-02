package com.travelmate.microservices.local;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.controller.AttractionController;
import com.travelmate.controller.CouponController;
import com.travelmate.controller.DestinationController;
import com.travelmate.controller.HotelController;
import com.travelmate.controller.ReplyController;
import com.travelmate.controller.ReviewController;
import com.travelmate.controller.ReviewReportController;
import com.travelmate.controller.TourProductController;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.AttractionOrder;
import com.travelmate.entity.Destination;
import com.travelmate.entity.HotelOrder;
import com.travelmate.mapper.DestinationMapper;
import com.travelmate.mapper.ReplyMapper;
import com.travelmate.mapper.ReviewReportMapper;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalEndpointContractTests {
    private HotelService hotelService;
    private HotelOrderService hotelOrderService;
    private AttractionService attractionService;
    private CouponService couponService;
    private ReviewService reviewService;
    private ReviewReportMapper reviewReportMapper;
    private ReplyMapper replyMapper;
    private DestinationMapper destinationMapper;
    private TourProductService tourProductService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        hotelService = mock(HotelService.class);
        hotelOrderService = mock(HotelOrderService.class);
        attractionService = mock(AttractionService.class);
        couponService = mock(CouponService.class);
        reviewService = mock(ReviewService.class);
        reviewReportMapper = mock(ReviewReportMapper.class);
        replyMapper = mock(ReplyMapper.class);
        destinationMapper = mock(DestinationMapper.class);
        tourProductService = mock(TourProductService.class);
        userContext = mock(UserContext.class);

        HotelController hotel = new HotelController();
        ReflectionTestUtils.setField(hotel, "hotelService", hotelService);
        ReflectionTestUtils.setField(hotel, "hotelOrderService", hotelOrderService);
        ReflectionTestUtils.setField(hotel, "userContext", userContext);
        AttractionController attraction = new AttractionController();
        ReflectionTestUtils.setField(attraction, "attractionService", attractionService);
        ReflectionTestUtils.setField(attraction, "userContext", userContext);
        CouponController coupon = new CouponController();
        ReflectionTestUtils.setField(coupon, "couponService", couponService);
        ReflectionTestUtils.setField(coupon, "userContext", userContext);
        ReviewController review = new ReviewController();
        ReflectionTestUtils.setField(review, "reviewService", reviewService);
        ReflectionTestUtils.setField(review, "userContext", userContext);
        ReviewReportController report = new ReviewReportController();
        ReflectionTestUtils.setField(report, "reportMapper", reviewReportMapper);
        ReflectionTestUtils.setField(report, "userContext", userContext);
        ReplyController reply = new ReplyController();
        ReflectionTestUtils.setField(reply, "replyMapper", replyMapper);
        ReflectionTestUtils.setField(reply, "userContext", userContext);
        DestinationController destination = new DestinationController();
        ReflectionTestUtils.setField(destination, "destinationMapper", destinationMapper);
        TourProductController tour = new TourProductController();
        ReflectionTestUtils.setField(tour, "tourProductService", tourProductService);

        mockMvc = MockMvcBuilders.standaloneSetup(hotel, attraction, coupon, review, report, reply, destination, tour)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void localEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        HotelOrder hotelOrder = new HotelOrder(); hotelOrder.setOrderNo("H-1");
        Attraction attraction = new Attraction(); attraction.setId(11L); attraction.setStatus(1);
        AttractionOrder attractionOrder = new AttractionOrder(); attractionOrder.setOrderNo("A-1");
        Destination destination = new Destination(); destination.setId(1L); destination.setSlug("beijing");
        when(hotelService.searchHotels(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(hotelService.getHotelWithRooms(1L)).thenReturn(Map.of("id", 1L));
        when(hotelService.getRoomsByHotelId(1L)).thenReturn(List.of());
        when(hotelOrderService.createOrder(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn("H-1");
        when(hotelOrderService.payOrder(7L, "H-1")).thenReturn(true);
        when(hotelOrderService.cancelOrder(7L, "H-1")).thenReturn(true);
        when(hotelOrderService.requestRefund(7L, "H-1")).thenReturn(true);
        when(hotelOrderService.getUserOrders(7L)).thenReturn(List.of(hotelOrder));
        when(hotelOrderService.getOrderDetail(7L, "H-1")).thenReturn(hotelOrder);
        when(attractionService.searchAttractions("北京")).thenReturn(List.of(attraction));
        when(attractionService.getById(11L)).thenReturn(attraction);
        when(attractionService.buyTicket(7L, 11L, 1, 0, "旅客", "13800138000")).thenReturn("A-1");
        when(attractionService.getUserTicketOrders(7L)).thenReturn(List.of(attractionOrder));
        when(attractionService.getTicketOrderDetail(7L, "A-1")).thenReturn(attractionOrder);
        when(couponService.listAvailable(7L)).thenReturn(List.of());
        when(couponService.listMyCoupons(7L)).thenReturn(List.of());
        when(couponService.claimCoupon(7L, 3L)).thenReturn("领取成功");
        when(reviewService.getReviews(11L, 0)).thenReturn(List.of());
        when(reviewReportMapper.selectCount(any())).thenReturn(0L);
        when(replyMapper.selectList(any())).thenReturn(List.of());
        when(destinationMapper.selectList(any())).thenReturn(List.of(destination));
        when(destinationMapper.selectOne(any())).thenReturn(destination);
        when(tourProductService.listByType(0)).thenReturn(List.of());

        mockMvc.perform(get("/api/hotel/search").param("city", "北京")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/hotel/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mockMvc.perform(get("/api/hotel/1/rooms")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/hotel/order/create").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("H-1"));
        mockMvc.perform(post("/api/hotel/order/H-1/pay")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/hotel/order/H-1/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/hotel/order/H-1/refund")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/hotel/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].orderNo").value("H-1"));
        mockMvc.perform(get("/api/hotel/order/H-1/receipt")).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderNo").value("H-1"));
        mockMvc.perform(get("/api/attraction/search").param("city", "北京")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/attraction/11")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(11));
        mockMvc.perform(post("/api/attraction/11/ticket").contentType("application/json")
                        .content("{\"adultCount\":1,\"childCount\":0,\"guestName\":\"旅客\",\"guestPhone\":\"13800138000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("A-1"));
        mockMvc.perform(get("/api/attraction/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].orderNo").value("A-1"));
        mockMvc.perform(get("/api/attraction/order/A-1/receipt")).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderNo").value("A-1"));
        mockMvc.perform(get("/api/coupon/list")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/coupon/my")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/coupon/claim/3")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value("领取成功"));
        mockMvc.perform(post("/api/review/add").contentType("application/json")
                        .content("{\"targetId\":11,\"targetType\":0,\"rating\":5,\"content\":\"很好\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/review/list").param("targetId", "11").param("targetType", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/review/report").contentType("application/json").content("{\"reviewId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/reply/list").param("reviewId", "2")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/reply/add").contentType("application/json").content("{\"reviewId\":2,\"content\":\"回复\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/destinations")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].slug").value("beijing"));
        mockMvc.perform(get("/api/destinations/beijing")).andExpect(status().isOk()).andExpect(jsonPath("$.data.slug").value("beijing"));
        mockMvc.perform(get("/api/tour/list").param("type", "0")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void localEndpointsEnforceAuthenticationBoundaries() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);
        when(hotelService.searchHotels(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(hotelService.getHotelWithRooms(1L)).thenReturn(Map.of());
        when(hotelService.getRoomsByHotelId(1L)).thenReturn(List.of());
        Attraction attraction = new Attraction(); attraction.setId(11L); attraction.setStatus(1);
        when(attractionService.searchAttractions(null)).thenReturn(List.of());
        when(attractionService.getById(11L)).thenReturn(attraction);
        when(couponService.listAvailable(null)).thenReturn(List.of());
        when(reviewService.getReviews(11L, 0)).thenReturn(List.of());
        when(replyMapper.selectList(any())).thenReturn(List.of());
        Destination destination = new Destination(); destination.setSlug("beijing");
        when(destinationMapper.selectList(any())).thenReturn(List.of(destination));
        when(destinationMapper.selectOne(any())).thenReturn(destination);
        when(tourProductService.listByType(0)).thenReturn(List.of());

        for (String path : List.of("/api/hotel/search", "/api/hotel/1", "/api/hotel/1/rooms",
                "/api/attraction/search", "/api/attraction/11", "/api/coupon/list", "/api/destinations",
                "/api/destinations/beijing", "/api/tour/list?type=0")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/review/list").param("targetId", "11").param("targetType", "0")).andExpect(status().isOk());
        mockMvc.perform(get("/api/reply/list").param("reviewId", "2")).andExpect(status().isOk());
        for (String path : List.of("/api/hotel/orders", "/api/hotel/order/H-1/receipt", "/api/attraction/orders",
                "/api/attraction/order/A-1/receipt", "/api/coupon/my")) {
            mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        }
        for (String path : List.of("/api/hotel/order/H-1/pay", "/api/hotel/order/H-1/cancel", "/api/hotel/order/H-1/refund",
                "/api/coupon/claim/3")) {
            mockMvc.perform(post(path)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        }
        mockMvc.perform(post("/api/hotel/order/create").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/attraction/11/ticket").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/review/add").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/review/report").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/reply/add").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void localEndpointsRejectMalformedParameters() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(destinationMapper.selectOne(any())).thenReturn(null);
        mockMvc.perform(get("/api/hotel/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/hotel/not-a-number/rooms")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/hotel/search").param("starRating", "bad")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/hotel/order/create")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/attraction/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/attraction/not-a-number/ticket").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/coupon/claim/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/review/add").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/review/list").param("targetId", "11")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/review/report").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/reply/list")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reply/add")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/destinations/missing")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/tour/list").param("type", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/tour/list").param("type", "bad")).andExpect(status().isBadRequest());
        for (String path : List.of("/api/hotel/order/missing/pay", "/api/hotel/order/missing/cancel",
                "/api/hotel/order/missing/refund", "/api/hotel/order/missing/receipt", "/api/attraction/order/missing/receipt")) {
            mockMvc.perform(path.endsWith("receipt") ? get(path) : post(path))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        }
    }
}
