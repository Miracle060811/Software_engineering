package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Review;
import com.travelmate.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器 (成员B负责)
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserContext userContext;

    /**
     * 添加评价
     * POST /api/review/add
     */
    @PostMapping("/add")
    public Result<String> addReview(@RequestBody Review review) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录或Token无效");
        }

        // 强制使用当前登录用户ID，防止篡改
        review.setUserId(userId);

        if (review.getTargetId() == null || review.getTargetType() == null) {
            return Result.error("评价目标不能为空");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return Result.error("评分必须在1到5之间");
        }

        try {
            reviewService.addReview(review);
            return Result.success("评价成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取评价列表
     * GET /api/review/list?targetId=&targetType=
     */
    @GetMapping("/list")
    public Result<List<Review>> getReviews(
            @RequestParam Long targetId,
            @RequestParam Integer targetType) {
        return Result.success(reviewService.getReviews(targetId, targetType));
    }

    // ===================== 工具方法 =====================

    private Long getCurrentUserId() {
        return userContext.getCurrentUserIdOrNull();
    }
}
