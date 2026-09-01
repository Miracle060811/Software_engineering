package com.travelmate.microservices.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.entity.Follow;
import com.travelmate.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/internal/identity/community")
public class InternalCommunityIdentityController {
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final String serviceToken;

    public InternalCommunityIdentityController(UserMapper userMapper, FollowMapper followMapper,
                                               @Value("${app.internal-service-token}") String serviceToken) {
        this.userMapper = userMapper;
        this.followMapper = followMapper;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/users/{id}")
    public UserSummary user(@PathVariable Long id, @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        User user = userMapper.selectById(id);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        return summary(user);
    }

    @GetMapping("/users")
    public List<UserSummary> users(@RequestParam Collection<Long> ids,
                                   @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        if (ids.isEmpty()) return List.of();
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids))
                .stream().map(this::summary).toList();
    }

    @GetMapping("/follows/status")
    public boolean follows(@RequestParam Long followerId, @RequestParam Long followeeId,
                           @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId)) > 0;
    }

    @GetMapping("/following/{userId}")
    public List<Long> following(@PathVariable Long userId,
                                @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        return followMapper.selectList(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId))
                .stream().map(Follow::getFolloweeId).toList();
    }

    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    private void verify(String token) {
        if (!serviceToken.equals(token)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }

    public record UserSummary(Long id, String username, String nickname, String avatar) {}
}
