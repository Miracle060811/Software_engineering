package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Like;
import com.travelmate.entity.Post;
import com.travelmate.mapper.LikeMapper;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/like")
public class LikeController {

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * POST /api/like/toggle - 点赞/取消点赞（targetId, targetType[0-游记,1-评论,2-收藏]）
     */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long targetId = Long.parseLong(body.get("targetId").toString());
        Integer targetType = Integer.parseInt(body.get("targetType").toString());

        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId)
                .eq(Like::getTargetType, targetType);
        Like existing = likeMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            // 取消点赞/收藏
            likeMapper.deleteById(existing.getId());
            // 更新游记计数
            if (targetType == 0) {
                updatePostCount(targetId, "like_count", -1);
            } else if (targetType == 2) {
                updatePostCount(targetId, "collect_count", -1);
            }
            result.put("liked", false);
        } else {
            // 点赞/收藏
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
            // 更新游记计数
            if (targetType == 0) {
                updatePostCount(targetId, "like_count", 1);
            } else if (targetType == 2) {
                updatePostCount(targetId, "collect_count", 1);
            }
            result.put("liked", true);
        }
        return Result.success(result);
    }

    /**
     * GET /api/like/status?targetId=&targetType= - 是否已点赞
     */
    @GetMapping("/status")
    public Result<Boolean> likeStatus(
            @RequestParam Long targetId,
            @RequestParam Integer targetType) {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetId, targetId)
                .eq(Like::getTargetType, targetType);
        boolean liked = likeMapper.selectCount(wrapper) > 0;
        return Result.success(liked);
    }

    // ======================== 工具方法 ========================

    private void updatePostCount(Long postId, String column, int delta) {
        LambdaUpdateWrapper<Post> upd = new LambdaUpdateWrapper<>();
        upd.eq(Post::getId, postId);
        if (delta > 0) {
            upd.setSql(column + " = " + column + " + 1");
        } else {
            upd.setSql(column + " = GREATEST(" + column + " - 1, 0)");
        }
        postMapper.update(null, upd);
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
