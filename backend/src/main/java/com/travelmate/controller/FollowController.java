package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/follow")
public class FollowController {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * POST /api/follow/{userId} - 关注/取关（toggle逻辑）
     */
    @PostMapping("/{userId}")
    public Result<Map<String, Object>> toggleFollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId.equals(userId)) {
            return Result.error("不能关注自己");
        }

        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, userId);
        Follow existing = followMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            followMapper.deleteById(existing.getId());
            result.put("followed", false);
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(currentUserId);
            follow.setFolloweeId(userId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);
            result.put("followed", true);
        }
        return Result.success(result);
    }

    /**
     * GET /api/follow/fans/{userId} - 粉丝列表
     */
    @GetMapping("/fans/{userId}")
    public Result<List<Map<String, Object>>> fans(@PathVariable Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<Map<String, Object>> result = buildUserInfoList(follows, true);
        return Result.success(result);
    }

    /**
     * GET /api/follow/following/{userId} - 关注列表
     */
    @GetMapping("/following/{userId}")
    public Result<List<Map<String, Object>>> following(@PathVariable Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreateTime);
        List<Follow> follows = followMapper.selectList(wrapper);
        List<Map<String, Object>> result = buildUserInfoList(follows, false);
        return Result.success(result);
    }

    /**
     * GET /api/follow/status/{userId} - 是否已关注
     */
    @GetMapping("/status/{userId}")
    public Result<Boolean> followStatus(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, currentUserId)
                .eq(Follow::getFolloweeId, userId);
        boolean followed = followMapper.selectCount(wrapper) > 0;
        return Result.success(followed);
    }

    // ======================== 工具方法 ========================

    private List<Map<String, Object>> buildUserInfoList(List<Follow> follows, boolean getFans) {
        return follows.stream().map(f -> {
            Long targetId = getFans ? f.getFollowerId() : f.getFolloweeId();
            User user = userMapper.selectById(targetId);
            Map<String, Object> map = new HashMap<>();
            if (user != null) {
                map.put("userId", user.getId());
                map.put("username", user.getUsername());
                map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar());
            }
            map.put("createTime", f.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

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
