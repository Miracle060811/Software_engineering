package com.travelmate.microservices.traffic;

import com.travelmate.controller.FlightController;
import com.travelmate.controller.TrafficOrderController;
import com.travelmate.controller.TrainController;
import com.travelmate.common.UserContext;
import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.entity.Flight;
import com.travelmate.entity.TrafficOrder;
import com.travelmate.entity.Train;
import com.travelmate.service.FlightService;
import com.travelmate.service.TrafficOrderService;
import com.travelmate.service.TrainLiveSyncService;
import com.travelmate.service.TrainService;
import com.travelmate.service.TrainWaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrafficPublicApiTests {
    private FlightService flightService;
    private TrainService trainService;
    private TrafficOrderService trafficOrderService;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flightService = mock(FlightService.class);
        trainService = mock(TrainService.class);
        trafficOrderService = mock(TrafficOrderService.class);
        userContext = mock(UserContext.class);

        FlightController flightController = new FlightController();
        ReflectionTestUtils.setField(flightController, "flightService", flightService);

        TrainController trainController = new TrainController();
        ReflectionTestUtils.setField(trainController, "trainService", trainService);
        ReflectionTestUtils.setField(trainController, "trainLiveSyncService", mock(TrainLiveSyncService.class));
        ReflectionTestUtils.setField(trainController, "trainWaitlistService", mock(TrainWaitlistService.class));
        ReflectionTestUtils.setField(trainController, "userContext", userContext);

        TrafficOrderController orderController = new TrafficOrderController();
        ReflectionTestUtils.setField(orderController, "trafficOrderService", trafficOrderService);
        ReflectionTestUtils.setField(orderController, "userContext", userContext);

        mockMvc = MockMvcBuilders.standaloneSetup(flightController, trainController, orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

    @Test
    void trainSearchReturnsServiceOwnedData() throws Exception {
        Train train = new Train();
        train.setId(21L);
        train.setTrainNo("G101");
        when(trainService.searchTrains("北京南", "上海虹桥", "2026-09-01", 0, 20))
                .thenReturn(List.of(train));

        mockMvc.perform(get("/api/train/search")
                        .param("depStation", "北京南")
                        .param("arrStation", "上海虹桥")
                        .param("date", "2026-09-01")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[0].trainNo").value("G101"));
    }

    @Test
    void authenticatedUserCanCreateAndListTrafficOrders() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(trafficOrderService.createTrainOrder(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any())).thenReturn("TR202608310001");
        TrafficOrder order = new TrafficOrder();
        order.setOrderNo("TR202608310001");
        order.setUserId(7L);
        when(trafficOrderService.getUserOrders(7L)).thenReturn(List.of(order));

        mockMvc.perform(post("/api/order/train/create")
                        .contentType("application/json")
                        .content("{\"trainId\":21,\"seatType\":\"SecondClass\",\"ticketCount\":1,\"passengerId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("TR202608310001"));

        mockMvc.perform(get("/api/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderNo").value("TR202608310001"));

        verify(trafficOrderService).getUserOrders(7L);
    }

    @Test
    void identityOutageReturnsDesignedServiceUnavailableResponse() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(trafficOrderService.createTrainOrder(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "身份服务暂不可用，请稍后重试"));

        mockMvc.perform(post("/api/order/train/create")
                        .contentType("application/json")
                        .content("{\"trainId\":21,\"seatType\":\"SecondClass\",\"ticketCount\":1,\"passengerId\":9}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.msg").value("身份服务暂不可用，请稍后重试"));
    }
}
