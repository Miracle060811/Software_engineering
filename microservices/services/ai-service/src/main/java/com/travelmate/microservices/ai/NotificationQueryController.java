package com.travelmate.microservices.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.NotificationMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationQueryController {
    private final NotificationMapper notificationMapper;
    private final UserContext userContext;

    public NotificationQueryController(NotificationMapper notificationMapper, UserContext userContext) {
        this.notificationMapper = notificationMapper;
        this.userContext = userContext;
    }

    @GetMapping("/list")
    public Result<List<Notification>> list() {
        Long userId = userContext.getCurrentUserId();
        return Result.success(notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime)));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = userContext.getCurrentUserId();
        return Result.success(notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)));
    }

    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = userContext.getCurrentUserId();
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId));
        return Result.success();
    }

    @DeleteMapping("/clear-all")
    public Result<Void> clearAll() {
        Long userId = userContext.getCurrentUserId();
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId));
        return Result.success();
    }
}
