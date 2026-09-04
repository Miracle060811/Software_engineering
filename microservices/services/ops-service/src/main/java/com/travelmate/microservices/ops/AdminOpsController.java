package com.travelmate.microservices.ops;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.SysSensitiveWord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOpsController {
    private final OpsAggregationGateway gateway;
    private final OpsLocalService localService;
    private final UserContext userContext;
    private final AdminDashboardService dashboardService;
    private final AdminCsvImportService csvImportService;

    public AdminOpsController(OpsAggregationGateway gateway, OpsLocalService localService, UserContext userContext,
                              AdminDashboardService dashboardService, AdminCsvImportService csvImportService) {
        this.gateway = gateway;
        this.localService = localService;
        this.userContext = userContext;
        this.dashboardService = dashboardService;
        this.csvImportService = csvImportService;
    }

    @GetMapping("/stats") public Result<Map<String, Object>> stats() { return Result.success(gateway.stats()); }
    @GetMapping("/dashboard/data") public Result<Map<String, Object>> dashboard() { return Result.success(dashboardService.dashboard()); }
    @GetMapping("/users") public Result<List<Map<String, Object>>> users() { return Result.success(gateway.users()); }
    @PostMapping("/users/{id}/disable") public Result<Void> disableUser(@PathVariable Long id) { gateway.disableUser(id, userContext.getCurrentUserId()); return Result.success(); }
    @PostMapping("/users/{id}/enable") public Result<Void> enableUser(@PathVariable Long id) { gateway.enableUser(id); return Result.success(); }
    @GetMapping("/orders") public Result<List<Map<String, Object>>> orders() { return Result.success(gateway.orders()); }
    @GetMapping("/flights") public Result<List<Map<String, Object>>> flights() { return Result.success(gateway.flights()); }
    @PostMapping("/flights") public Result<Map<String, Object>> addFlight(@RequestBody Map<String, Object> body) { return Result.success(gateway.addFlight(body)); }
    @PutMapping("/flights/{id}") public Result<Void> updateFlight(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateFlight(id, body); return Result.success(); }
    @DeleteMapping("/flights/{id}") public Result<Void> deleteFlight(@PathVariable Long id) { gateway.deleteFlight(id); return Result.success(); }
    @GetMapping("/trains") public Result<List<Map<String, Object>>> trains() { return Result.success(gateway.trains()); }
    @PostMapping("/trains") public Result<Map<String, Object>> addTrain(@RequestBody Map<String, Object> body) { return Result.success(gateway.addTrain(body)); }
    @PutMapping("/trains/{id}") public Result<Void> updateTrain(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateTrain(id, body); return Result.success(); }
    @DeleteMapping("/trains/{id}") public Result<Void> deleteTrain(@PathVariable Long id) { gateway.deleteTrain(id); return Result.success(); }
    @GetMapping("/hotels") public Result<List<Map<String, Object>>> hotels() { return Result.success(gateway.hotels()); }
    @PostMapping("/hotels") public Result<Map<String, Object>> addHotel(@RequestBody Map<String, Object> body) { return Result.success(gateway.addHotel(body)); }
    @PutMapping("/hotels/{id}") public Result<Void> updateHotel(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateHotel(id, body); return Result.success(); }
    @DeleteMapping("/hotels/{id}") public Result<Void> deleteHotel(@PathVariable Long id) { gateway.deleteHotel(id); return Result.success(); }
    @GetMapping("/hotels/{hotelId}/rooms") public Result<List<Map<String, Object>>> hotelRooms(@PathVariable Long hotelId) { return Result.success(gateway.hotelRooms(hotelId)); }
    @PostMapping("/hotels/{hotelId}/rooms") public Result<Map<String, Object>> addHotelRoom(@PathVariable Long hotelId, @RequestBody Map<String, Object> body) { return Result.success(gateway.addHotelRoom(hotelId, body)); }
    @PutMapping("/hotel-rooms/{id}") public Result<Void> updateHotelRoom(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateHotelRoom(id, body); return Result.success(); }
    @DeleteMapping("/hotel-rooms/{id}") public Result<Void> deleteHotelRoom(@PathVariable Long id) { gateway.deleteHotelRoom(id); return Result.success(); }
    @GetMapping("/attractions") public Result<List<Map<String, Object>>> attractions() { return Result.success(gateway.attractions()); }
    @PostMapping("/attractions") public Result<Map<String, Object>> addAttraction(@RequestBody Map<String, Object> body) { return Result.success(gateway.addAttraction(body)); }
    @PutMapping("/attractions/{id}") public Result<Void> updateAttraction(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateAttraction(id, body); return Result.success(); }
    @DeleteMapping("/attractions/{id}") public Result<Void> deleteAttraction(@PathVariable Long id) { gateway.deleteAttraction(id); return Result.success(); }
    @GetMapping("/destinations") public Result<List<Map<String, Object>>> destinations() { return Result.success(gateway.destinations()); }
    @PostMapping("/destinations/sync-home") public Result<Map<String, Object>> syncHomeDestinations(@RequestBody List<Map<String, Object>> body) { return Result.success(gateway.syncHomeDestinations(body)); }
    @DeleteMapping("/destinations/{id}") public Result<Void> deleteDestination(@PathVariable Long id) { gateway.deleteDestination(id); return Result.success(); }
    @GetMapping("/coupons") public Result<List<Map<String, Object>>> coupons() { return Result.success(gateway.coupons()); }
    @PostMapping("/coupons") public Result<Map<String, Object>> addCoupon(@RequestBody Map<String, Object> body) { return Result.success(gateway.addCoupon(body)); }
    @PutMapping("/coupons/{id}") public Result<Void> updateCoupon(@PathVariable Long id, @RequestBody Map<String, Object> body) { gateway.updateCoupon(id, body); return Result.success(); }
    @DeleteMapping("/coupons/{id}") public Result<Void> deleteCoupon(@PathVariable Long id) { gateway.deleteCoupon(id); return Result.success(); }
    @GetMapping("/coupons/{id}/claims") public Result<List<Map<String, Object>>> couponClaims(@PathVariable Long id) { return Result.success(gateway.couponClaims(id)); }
    @PostMapping("/orders/{orderNo}/refund/approve") public Result<String> approveOrderRefund(@PathVariable String orderNo) { return Result.success(gateway.approveOrderRefund(orderNo)); }
    @PostMapping("/orders/{orderNo}/refund/reject") public Result<String> rejectOrderRefund(@PathVariable String orderNo) { return Result.success(gateway.rejectOrderRefund(orderNo)); }
    @PostMapping("/orders/{orderNo}/ticket/complete") public Result<String> completeOrderTicket(@PathVariable String orderNo) { return Result.success(gateway.completeOrderTicket(orderNo)); }
    @PostMapping(value="/import/{type}", consumes="multipart/form-data")
    public Result<Map<String,Object>> importCsv(@PathVariable String type, @RequestPart("file") MultipartFile file,
                                                @RequestParam(defaultValue="false") boolean dryRun,
                                                @RequestParam(defaultValue="insert") String mode) {
        try {
            return Result.success(csvImportService.importCsv(type, file, dryRun, mode));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
    @GetMapping("/posts") public Result<List<Map<String, Object>>> posts(@RequestParam(required = false) Integer status) { return Result.success(gateway.posts(status)); }

    @PostMapping("/posts/{id}/approve")
    public Result<Map<String, Object>> approvePost(@PathVariable Long id) {
        Map<String, Object> result = gateway.approvePost(id);
        localService.log(userContext.getCurrentUserId(), "审核通过游记: " + id, 1, null);
        return Result.success(result);
    }

    @PostMapping("/posts/{id}/reject")
    public Result<Map<String, Object>> rejectPost(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> result = gateway.rejectPost(id, body == null ? Map.of() : body);
        localService.log(userContext.getCurrentUserId(), "审核拒绝游记: " + id, 1, null);
        return Result.success(result);
    }

    @PostMapping("/posts/{id}/metrics")
    public Result<Map<String, Object>> updatePostMetrics(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(gateway.updatePostMetrics(id, body));
    }

    @GetMapping("/review-reports")
    public Result<List<Map<String, Object>>> reviewReports(@RequestParam(required = false) Integer status) {
        return Result.success(gateway.reviewReports(status));
    }

    @PostMapping("/review-reports/{id}/resolve")
    public Result<Map<String, Object>> resolveReport(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = gateway.resolveReport(id, body);
        localService.log(userContext.getCurrentUserId(), "处理评价举报: " + id, 1, null);
        return Result.success(result);
    }

    @PostMapping("/review-reports/{id}/reject")
    public Result<Map<String, Object>> rejectReport(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return Result.success(gateway.rejectReport(id, body == null ? Map.of() : body));
    }

    @PostMapping("/review-reports/{id}/delete-review")
    public Result<Map<String, Object>> deleteReportedReview(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return Result.success(gateway.deleteReportedReview(id, body == null ? Map.of() : body));
    }

    @GetMapping("/reviews/{reviewId}/replies")
    public Result<List<Map<String, Object>>> reviewReplies(@PathVariable Long reviewId) {
        return Result.success(gateway.reviewReplies(reviewId));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public Result<Map<String, Object>> addReviewReply(@PathVariable Long reviewId, @RequestBody Map<String, Object> body) {
        Map<String, Object> request = new java.util.LinkedHashMap<>(body);
        request.put("adminId", userContext.getCurrentUserId());
        return Result.success(gateway.addReviewReply(reviewId, request));
    }

    @DeleteMapping("/replies/{id}")
    public Result<Void> deleteReply(@PathVariable Long id) { gateway.deleteReply(id); return Result.success(); }

    @GetMapping("/sensitive-words")
    public Result<List<SysSensitiveWord>> sensitiveWords() { return Result.success(localService.listSensitiveWords()); }

    @PostMapping("/sensitive-words")
    public Result<SysSensitiveWord> addSensitiveWord(@RequestBody Map<String, Object> body) {
        Integer level = body.get("level") == null ? null : Integer.valueOf(body.get("level").toString());
        return Result.success(localService.addSensitiveWord((String) body.get("word"), level, userContext.getCurrentUserId()));
    }

    @PutMapping("/sensitive-words/{id}")
    public Result<Void> updateSensitiveWord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer level = body.get("level") == null ? null : Integer.valueOf(body.get("level").toString());
        localService.updateSensitiveWord(id, (String) body.get("word"), level, userContext.getCurrentUserId());
        return Result.success();
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        localService.deleteSensitiveWord(id, userContext.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<Map<String, Object>> logs(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return Result.success(localService.logs(page, size));
    }
}
