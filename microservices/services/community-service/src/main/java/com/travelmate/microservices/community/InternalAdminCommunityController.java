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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    public List<Post> posts(@RequestParam(required = false) Integer status,
                            @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>().orderByDesc(Post::getCreateTime);
        if (status != null) query.eq(Post::getStatus, status);
        return mapper.selectList(query);
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

    @PostMapping("/posts/{id}/reject")
    public Post reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                       @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        Post post = requiredPost(id);
        String reason = body == null || body.get("reason") == null
                ? "内容不符合社区规范" : body.get("reason").toString().trim();
        if (reason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "拒绝原因不能为空");
        post.setStatus(2); post.setRejectReason(reason); post.setUpdateTime(LocalDateTime.now());
        mapper.updateById(post);
        return post;
    }

    @PostMapping("/posts/{id}/metrics")
    public Post metrics(@PathVariable Long id, @RequestBody Map<String, Object> body,
                        @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        Post post = requiredPost(id);
        post.setLikeCount(nonNegative(body.get("likeCount"), "点赞量"));
        post.setCollectCount(nonNegative(body.get("collectCount"), "收藏量"));
        post.setUpdateTime(LocalDateTime.now());
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

    private Post requiredPost(Long id) {
        Post post = mapper.selectById(id);
        if (post == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "游记不存在");
        return post;
    }

    private int nonNegative(Object value, String name) {
        try {
            int number = Integer.parseInt(value == null ? "" : value.toString());
            if (number < 0) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + "必须是非负整数");
        }
    }
}
