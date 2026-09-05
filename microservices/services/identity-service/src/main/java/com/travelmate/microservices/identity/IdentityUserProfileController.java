package com.travelmate.microservices.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.Result;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class IdentityUserProfileController {
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final CommunityProfileGateway communityGateway;

    public IdentityUserProfileController(UserMapper userMapper, FollowMapper followMapper,
                                         CommunityProfileGateway communityGateway) {
        this.userMapper = userMapper;
        this.followMapper = followMapper;
        this.communityGateway = communityGateway;
    }

    @GetMapping("/profile/{username}")
    public Result<Map<String, Object>> profile(@PathVariable String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).eq(User::getDeleted, 0));
        if (user == null) throw new RuntimeException("用户不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId()); result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname()); result.put("avatar", user.getAvatar());
        result.put("bio", user.getBio()); result.put("level", user.getLevel());
        result.put("fansCount", followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, user.getId())));
        result.put("followingCount", followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, user.getId())));
        return Result.success(result);
    }

    @GetMapping("/profile/{username}/posts")
    public Result<List<Map<String, Object>>> publishedPosts(@PathVariable String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).eq(User::getDeleted, 0));
        if (user == null) throw new RuntimeException("用户不存在");
        return Result.success(communityGateway.publishedPosts(user.getId()));
    }
}
