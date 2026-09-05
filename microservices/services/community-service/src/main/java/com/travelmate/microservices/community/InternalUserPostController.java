package com.travelmate.microservices.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/internal/community/users")
public class InternalUserPostController {
    private final PostMapper postMapper;
    private final String serviceToken;

    public InternalUserPostController(PostMapper postMapper,
                                      @Value("${app.internal-service-token}") String serviceToken) {
        this.postMapper = postMapper;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/{userId}/posts")
    public List<Post> publishedPosts(@PathVariable Long userId,
                                     @RequestHeader("X-Internal-Token") String suppliedToken) {
        if (!serviceToken.equals(suppliedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        }
        return postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getCreateTime));
    }
}
