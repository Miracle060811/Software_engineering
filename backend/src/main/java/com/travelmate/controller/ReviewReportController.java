package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.ReviewReport;
import com.travelmate.mapper.ReviewReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/review/report")
public class ReviewReportController {

    @Autowired
    private ReviewReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @PostMapping
    public Result<String> report(@RequestBody ReviewReport report) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        if (report.getReviewId() == null) return Result.error("评价ID不能为空");

        Long count = reportMapper.selectCount(new LambdaQueryWrapper<ReviewReport>()
                .eq(ReviewReport::getReviewId, report.getReviewId())
                .eq(ReviewReport::getReporterId, userId));
        if (count > 0) return Result.error("您已举报过该评价");

        report.setReporterId(userId);
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return Result.success("举报已提交");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        if ("anonymousUser".equals(username)) return null;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
