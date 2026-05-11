package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.entity.Notification;
import com.travelmate.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.travelmate.annotation.RateLimiter;

@RestController
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private UserMapper userMapper;

    // ======================== AI 行程接口 ========================

    /**
     * POST /api/ai/plan/generate - 生成AI行程（需要认证）
     */
    @PostMapping("/api/ai/plan/generate")
    public Result<AiPlan> generatePlan(@RequestBody AiPlanCreateDTO dto) {
        Long userId = getCurrentUserId();
        AiPlan plan = aiService.generatePlan(dto, userId);
        return Result.success(plan);
    }

    /**
     * GET /api/ai/plan/list - 我的行程列表
     */
    @GetMapping("/api/ai/plan/list")
    public Result<List<AiPlan>> listMyPlans() {
        Long userId = getCurrentUserId();
        return Result.success(aiService.listMyPlans(userId));
    }

    /**
     * GET /api/ai/plan/{id} - 行程详情
     */
    @GetMapping("/api/ai/plan/{id}")
    public Result<AiPlan> getPlan(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return Result.success(aiService.getPlanById(id, userId));
    }

    /**
     * POST /api/ai/chat - AI客服对话
     */
    @RateLimiter(maxRequests = 10, timeWindowSeconds = 1)
    @PostMapping("/api/ai/chat")
    public Result<AiChat> chat(@RequestBody AiChatDTO dto) {
        Long userId = getCurrentUserId();
        AiChat reply = aiService.chat(dto, userId);
        return Result.success(reply);
    }

    // ======================== 通知接口 ========================

    /**
     * GET /api/notification/list - 通知列表（需认证）
     */
    @GetMapping("/api/notification/list")
    public Result<List<Notification>> listNotifications() {
        Long userId = getCurrentUserId();
        return Result.success(aiService.listNotifications(userId));
    }

    /**
     * POST /api/notification/read/{id} - 标记已读
     */
    @PostMapping("/api/notification/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        aiService.markRead(id, userId);
        return Result.success();
    }

    /**
     * GET /api/notification/unread-count - 未读通知数
     */
    @GetMapping("/api/notification/unread-count")
    public Result<Long> unreadCount() {
        Long userId = getCurrentUserId();
        return Result.success(aiService.unreadCount(userId));
    }

    // ======================== 工具方法 ========================

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }
}
