package com.travelmate.microservices.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/internal/community/admin")
public class InternalAdminCommunityController {
    private final PostMapper mapper;
    private final String token;

    public InternalAdminCommunityController(PostMapper mapper, @Value("${app.internal-service-token}") String token) {
        this.mapper = mapper;
        this.token = token;
    }

    @GetMapping("/posts")
    public List<Post> posts(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectList(new LambdaQueryWrapper<Post>().orderByDesc(Post::getCreateTime));
    }

    @PostMapping("/posts/{id}/approve")
    public Post approve(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        Post post = mapper.selectById(id);
        if (post == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "游记不存在");
        post.setStatus(1); post.setRejectReason(null); post.setUpdateTime(LocalDateTime.now());
        mapper.updateById(post);
        return post;
    }

    @GetMapping("/pending-post-count")
    public long pendingCount(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0));
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }
}
