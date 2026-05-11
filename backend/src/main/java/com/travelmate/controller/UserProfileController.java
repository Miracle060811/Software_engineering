package com.travelmate.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Follow;
import com.travelmate.entity.Post;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private FollowMapper followMapper;

    /**
     * GET /api/user/profile/{username} - 获取用户主页信息（帖子数、关注数、粉丝数）
     */
    @GetMapping("/profile/{username}")
    public Result<Map<String, Object>> getUserProfile(@PathVariable String username) {
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getUsername, username);
        User user = userMapper.selectOne(userQuery);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 帖子数
        LambdaQueryWrapper<Post> postQuery = new LambdaQueryWrapper<>();
        postQuery.eq(Post::getUserId, user.getId()).eq(Post::getStatus, 1);
        long postCount = postMapper.selectCount(postQuery);

        // 关注数（该用户关注了多少人）
        LambdaQueryWrapper<Follow> followingQuery = new LambdaQueryWrapper<>();
        followingQuery.eq(Follow::getFollowerId, user.getId());
        long followingCount = followMapper.selectCount(followingQuery);

        // 粉丝数
        LambdaQueryWrapper<Follow> fansQuery = new LambdaQueryWrapper<>();
        fansQuery.eq(Follow::getFolloweeId, user.getId());
        long fansCount = followMapper.selectCount(fansQuery);

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        profile.put("postCount", postCount);
        profile.put("followingCount", followingCount);
        profile.put("fansCount", fansCount);

        return Result.success(profile);
    }

    /**
     * PUT /api/user/profile/update - 更新个人信息（nickname, avatar, email, phone）
     */
    @PutMapping("/profile/update")
    public Result<Void> updateProfile(@RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username);
        User user = userMapper.selectOne(query);
        if (user == null) {
            return Result.error("用户不存在");
        }

        LambdaUpdateWrapper<User> upd = new LambdaUpdateWrapper<>();
        upd.eq(User::getId, user.getId());

        if (body.containsKey("nickname")) {
            upd.set(User::getNickname, body.get("nickname"));
        }
        if (body.containsKey("avatar")) {
            upd.set(User::getAvatar, body.get("avatar"));
        }
        if (body.containsKey("email")) {
            upd.set(User::getEmail, body.get("email"));
        }
        if (body.containsKey("phone")) {
            upd.set(User::getPhone, body.get("phone"));
        }
        if (body.containsKey("bio")) {
            upd.set(User::getBio, body.get("bio"));
        }

        userMapper.update(null, upd);
        return Result.success();
    }
}
