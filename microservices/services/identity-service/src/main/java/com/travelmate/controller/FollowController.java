package com.travelmate.controller;

import com.travelmate.backend.entity.User;
import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/{userId}")
    public Result<Map<String, Object>> toggleFollow(@PathVariable Long userId) {
        int result = followService.toggleFollow(userContext.getCurrentUserId(), userId);
        Map<String, Object> map = new HashMap<>();
        map.put("followed", result == 1);
        return Result.success(map);
    }

    @GetMapping("/fans/{userId}")
    public Result<List<Map<String, Object>>> fans(@PathVariable Long userId) {
        List<User> fans = followService.fans(userId);
        return Result.success(fans.stream().map(this::userToMap).collect(Collectors.toList()));
    }

    @GetMapping("/following/{userId}")
    public Result<List<Map<String, Object>>> following(@PathVariable Long userId) {
        List<User> followings = followService.following(userId);
        return Result.success(followings.stream().map(this::userToMap).collect(Collectors.toList()));
    }

    @GetMapping("/status/{userId}")
    public Result<Boolean> followStatus(@PathVariable Long userId) {
        int status = followService.followStatus(userContext.getCurrentUserId(), userId);
        return Result.success(status > 0);
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        return map;
    }
}
