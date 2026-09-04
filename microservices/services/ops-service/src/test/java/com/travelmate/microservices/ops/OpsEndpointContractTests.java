package com.travelmate.microservices.ops;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.entity.SysSensitiveWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpsEndpointContractTests {
    private OpsAggregationGateway gateway;
    private OpsLocalService localService;
    private UserContext userContext;
    private AdminDashboardService dashboardService;
    private AdminCsvImportService csvImportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gateway = mock(OpsAggregationGateway.class);
        localService = mock(OpsLocalService.class);
        userContext = mock(UserContext.class);
        dashboardService = mock(AdminDashboardService.class);
        csvImportService = mock(AdminCsvImportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminOpsController(gateway, localService, userContext, dashboardService, csvImportService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void adminOpsEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.stats()).thenReturn(Map.of("totalUsers", 2L, "totalOrders", 3L));
        when(dashboardService.dashboard()).thenReturn(Map.ofEntries(
                Map.entry("totalUsers", 2L), Map.entry("totalOrders", 3L), Map.entry("todayOrders", 1L),
                Map.entry("pendingPosts", 0L), Map.entry("todayGmv", 99), Map.entry("onlineUsers", 1L),
                Map.entry("dailyTrend", List.of()), Map.entry("hotDestinations", List.of()),
                Map.entry("orderTypeDist", List.of()), Map.entry("userGrowth", List.of()),
                Map.entry("qpsTrend", List.of()), Map.entry("latencyTrend", List.of()),
                Map.entry("recentErrors", List.of()), Map.entry("alerts", List.of())));
        when(gateway.users()).thenReturn(List.of(Map.of("id", 1L)));
        when(gateway.orders()).thenReturn(List.of(Map.of("id", 2L)));
        when(gateway.flights()).thenReturn(List.of(Map.of("id", 3L)));
        when(gateway.posts(null)).thenReturn(List.of(Map.of("id", 4L)));
        when(gateway.approvePost(4L)).thenReturn(Map.of("id", 4L, "status", 1));
        when(gateway.reviewReports(null)).thenReturn(List.of(Map.of("id", 5L)));
        when(gateway.resolveReport(org.mockito.ArgumentMatchers.eq(5L), any())).thenReturn(Map.of("id", 5L, "status", 1));
        SysSensitiveWord word = new SysSensitiveWord(); word.setId(6L); word.setWord("风险词");
        when(localService.listSensitiveWords()).thenReturn(List.of(word));
        when(localService.addSensitiveWord("风险词", 2, 1L)).thenReturn(word);
        when(localService.logs(1, 20)).thenReturn(Map.of("records", List.of(), "total", 0, "page", 1, "size", 20));

        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalUsers").value(2));
        mockMvc.perform(get("/api/admin/dashboard/data")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(2))
                .andExpect(jsonPath("$.data.dailyTrend").isArray())
                .andExpect(jsonPath("$.data.alerts").isArray());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(post("/api/admin/users/7/disable")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/users/7/enable")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(2));
        mockMvc.perform(get("/api/admin/flights")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(3));
        mockMvc.perform(get("/api/admin/posts")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(4));
        mockMvc.perform(post("/api/admin/posts/4/approve")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mockMvc.perform(get("/api/admin/review-reports")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(5));
        mockMvc.perform(post("/api/admin/review-reports/5/resolve").contentType("application/json")
                        .content("{\"remark\":\"已处理\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mockMvc.perform(get("/api/admin/sensitive-words")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(6));
        mockMvc.perform(post("/api/admin/sensitive-words").contentType("application/json")
                        .content("{\"word\":\"风险词\",\"level\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.word").value("风险词"));
        mockMvc.perform(put("/api/admin/sensitive-words/6").contentType("application/json")
                        .content("{\"word\":\"新风险词\",\"level\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/sensitive-words/6")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/admin/logs")).andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void adminOpsEndpointsRejectMalformedParameters() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.stats()).thenReturn(Map.of());
        when(dashboardService.dashboard()).thenReturn(Map.of());
        when(gateway.users()).thenReturn(List.of());
        when(gateway.orders()).thenReturn(List.of());
        when(gateway.flights()).thenReturn(List.of());
        when(gateway.posts(null)).thenReturn(List.of());
        when(localService.listSensitiveWords()).thenReturn(List.of());
        for (String path : List.of("/api/admin/stats", "/api/admin/dashboard/data", "/api/admin/users", "/api/admin/orders", "/api/admin/flights",
                "/api/admin/posts", "/api/admin/sensitive-words")) {
            mockMvc.perform(get(path).param("unexpected", "ignored")).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/admin/posts/not-a-number/approve")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/users/not-a-number/disable")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/review-reports").param("status", "bad")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/review-reports/not-a-number/resolve")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/sensitive-words")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/sensitive-words/not-a-number").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/admin/sensitive-words/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/logs").param("page", "bad")).andExpect(status().isBadRequest());
    }

    @Test
    void downstreamOutageReturnsServiceUnavailableForAdminAggregation() throws Exception {
        ResponseStatusException unavailable = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "业务服务暂不可用，请稍后重试");
        when(gateway.stats()).thenThrow(unavailable);
        when(gateway.users()).thenThrow(unavailable);
        when(gateway.orders()).thenThrow(unavailable);
        when(gateway.flights()).thenThrow(unavailable);
        when(gateway.posts(null)).thenThrow(unavailable);
        when(gateway.approvePost(4L)).thenThrow(unavailable);
        when(gateway.reviewReports(null)).thenThrow(unavailable);
        when(gateway.resolveReport(org.mockito.ArgumentMatchers.eq(5L), any())).thenThrow(unavailable);
        for (String path : List.of("/api/admin/stats", "/api/admin/users", "/api/admin/orders", "/api/admin/flights",
                "/api/admin/posts", "/api/admin/review-reports")) {
            mockMvc.perform(get(path)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        }
        mockMvc.perform(post("/api/admin/posts/4/approve"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        mockMvc.perform(post("/api/admin/review-reports/5/resolve").contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }

    @Test
    void contentAdminEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(gateway.posts(2)).thenReturn(List.of(Map.of("id", 4L, "status", 2)));
        when(gateway.rejectPost(eq(4L), any())).thenReturn(Map.of("id", 4L, "status", 2));
        when(gateway.updatePostMetrics(eq(4L), any())).thenReturn(Map.of("id", 4L, "likeCount", 12));
        when(gateway.rejectReport(eq(5L), any())).thenReturn(Map.of("id", 5L, "status", 1));
        when(gateway.deleteReportedReview(eq(5L), any())).thenReturn(Map.of("id", 5L, "status", 1));
        when(gateway.reviewReplies(8L)).thenReturn(List.of(Map.of("id", 9L)));
        when(gateway.addReviewReply(eq(8L), any())).thenReturn(Map.of("id", 10L, "content", "感谢反馈"));

        mockMvc.perform(get("/api/admin/posts").param("status", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value(2));
        mockMvc.perform(post("/api/admin/posts/4/reject").contentType("application/json")
                        .content("{\"reason\":\"需要修改\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(2));
        mockMvc.perform(post("/api/admin/posts/4/metrics").contentType("application/json")
                        .content("{\"likeCount\":12,\"collectCount\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.likeCount").value(12));
        mockMvc.perform(post("/api/admin/review-reports/5/reject").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/review-reports/5/delete-review").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/reviews/8/replies"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(9));
        mockMvc.perform(post("/api/admin/reviews/8/replies").contentType("application/json")
                        .content("{\"content\":\"感谢反馈\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(10));
        mockMvc.perform(delete("/api/admin/replies/9")).andExpect(status().isOk());
    }

    @Test
    void transportAdminEndpointsExposeNormalContracts() throws Exception {
        when(gateway.trains()).thenReturn(List.of(Map.of("id", 2L)));
        when(gateway.addFlight(any())).thenReturn(Map.of("id", 1L));
        when(gateway.addTrain(any())).thenReturn(Map.of("id", 2L));
        mockMvc.perform(get("/api/admin/trains")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/flights").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/flights/1").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/flights/1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/trains").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/trains/2").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/trains/2")).andExpect(status().isOk());
    }

    @Test void hotelAdminEndpointsExposeNormalContracts() throws Exception {
        when(gateway.hotels()).thenReturn(List.of(Map.of("id",1L)));
        when(gateway.hotelRooms(1L)).thenReturn(List.of(Map.of("id",2L)));
        when(gateway.addHotel(any())).thenReturn(Map.of("id",1L));
        when(gateway.addHotelRoom(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(Map.of("id",2L));
        mockMvc.perform(get("/api/admin/hotels")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/hotels").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/hotels/1").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/hotels/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/hotels/1/rooms")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/hotels/1/rooms").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/hotel-rooms/2").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/hotel-rooms/2")).andExpect(status().isOk());
    }

    @Test void attractionAdminEndpointsExposeNormalContracts() throws Exception {
        when(gateway.attractions()).thenReturn(List.of(Map.of("id",3L)));
        when(gateway.addAttraction(any())).thenReturn(Map.of("id",3L));
        mockMvc.perform(get("/api/admin/attractions")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/attractions").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/attractions/3").contentType("application/json").content("{}")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/attractions/3")).andExpect(status().isOk());
    }

    @Test void destinationAdminEndpointsExposeNormalContracts() throws Exception {
        when(gateway.destinations()).thenReturn(List.of(Map.of("id",7L)));
        when(gateway.syncHomeDestinations(any())).thenReturn(Map.of("total",1,"inserted",1,"updated",0));
        mockMvc.perform(get("/api/admin/destinations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7));
        mockMvc.perform(post("/api/admin/destinations/sync-home").contentType("application/json").content("[{}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.inserted").value(1));
        mockMvc.perform(delete("/api/admin/destinations/7")).andExpect(status().isOk());
    }

    @Test void couponAdminEndpointsExposeNormalContracts() throws Exception {
        when(gateway.coupons()).thenReturn(List.of(Map.of("id",3L)));
        when(gateway.addCoupon(any())).thenReturn(Map.of("id",3L));
        when(gateway.couponClaims(3L)).thenReturn(List.of(Map.of("id",8L,"userId",9L,"username","member")));
        mockMvc.perform(get("/api/admin/coupons")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/coupons").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(3));
        mockMvc.perform(put("/api/admin/coupons/3").contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/coupons/3")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/coupons/3/claims")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("member"));
    }

    @Test void orderActionEndpointsExposeNormalContracts() throws Exception {
        when(gateway.approveOrderRefund("T1")).thenReturn("退款审批已通过");
        when(gateway.rejectOrderRefund("T1")).thenReturn("退款申请已驳回");
        when(gateway.completeOrderTicket("T1")).thenReturn("出票已完成");
        mockMvc.perform(post("/api/admin/orders/T1/refund/approve")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("退款审批已通过"));
        mockMvc.perform(post("/api/admin/orders/T1/refund/reject")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/orders/T1/ticket/complete")).andExpect(status().isOk());
    }

    @Test void csvImportEndpointExposesNormalContract() throws Exception {
        when(csvImportService.importCsv(eq("flights"),any(),eq(true),eq("insert")))
                .thenReturn(Map.of("total",1,"success",1,"failed",0,"validated",1));
        MockMultipartFile file=new MockMultipartFile("file","flights.csv","text/csv","header\nvalue".getBytes());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/admin/import/flights")
                        .file(file).param("dryRun","true").param("mode","insert"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.validated").value(1));
    }

    @Test void csvImportEndpointRejectsInvalidCsv() throws Exception {
        when(csvImportService.importCsv(eq("flights"), any(), eq(false), eq("insert")))
                .thenThrow(new IllegalArgumentException("CSV 缺少必填列: flightNo"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "flights.csv", "text/csv", "airline\n国航".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/admin/import/flights")
                        .file(file).param("dryRun", "false").param("mode", "insert"))
                .andExpect(status().isBadRequest());
    }
}
