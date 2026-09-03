package com.travelmate.controller;

import com.travelmate.annotation.RateLimiter;
import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.dto.TourBookingCreateDTO;
import com.travelmate.entity.TourOrder;
import com.travelmate.entity.TourProduct;
import com.travelmate.service.TourProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tour")
public class TourProductController {

    @Autowired
    private TourProductService tourProductService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/list")
    public Result<List<TourProduct>> list(@RequestParam(defaultValue = "0") Integer type) {
        if (type == null || (type != 0 && type != 1)) {
            return Result.error("游览产品类型必须为0或1");
        }
        return Result.success(tourProductService.listByType(type));
    }

    @RateLimiter(maxRequests = 5, timeWindowSeconds = 1)
    @PostMapping("/orders")
    public Result<TourOrder> createOrder(
            @RequestBody TourBookingCreateDTO request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        Long userId = userContext.getCurrentUserIdOrNull();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        if (request != null && (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank())) {
            request.setIdempotencyKey(idempotencyKey);
        }
        try {
            return Result.success(tourProductService.createBooking(userId, request));
        } catch (DuplicateKeyException exception) {
            try {
                return Result.success(tourProductService.findIdempotentBooking(userId, request));
            } catch (RuntimeException retryException) {
                return Result.error(retryException.getMessage());
            }
        } catch (RuntimeException exception) {
            return Result.error(exception.getMessage());
        }
    }

    @GetMapping("/orders")
    public Result<List<TourOrder>> getMyOrders() {
        Long userId = userContext.getCurrentUserIdOrNull();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        return Result.success(tourProductService.listUserOrders(userId));
    }

    @GetMapping("/orders/{orderNo}")
    public Result<TourOrder> getMyOrder(@PathVariable String orderNo) {
        Long userId = userContext.getCurrentUserIdOrNull();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }
        TourOrder order = tourProductService.getUserOrder(userId, orderNo);
        return order == null ? Result.error("订单不存在或无权查看") : Result.success(order);
    }
}
