package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/like")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserContext userContext;

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Object> body) {
        return Result.success(likeService.toggleLike(userContext.getCurrentUserId(), body));
    }

    @GetMapping("/status")
    public Result<Boolean> likeStatus(
            @RequestParam Long targetId,
            @RequestParam Integer targetType) {
        int liked = likeService.likeStatus(userContext.getCurrentUserId(), targetId, targetType);
        return Result.success(liked > 0);
    }
}
