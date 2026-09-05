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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/community/admin")
public class InternalAdminCommunityController {
    private static final Logger log = LoggerFactory.getLogger(InternalAdminCommunityController.class);
    private final PostMapper mapper;
    private final CommunityPostAuditGateway auditGateway;
    private final String token;

    public InternalAdminCommunityController(PostMapper mapper, CommunityPostAuditGateway auditGateway,
                                            @Value("${app.internal-service-token}") String token) {
        this.mapper = mapper;
        this.auditGateway = auditGateway;
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
        notifyAudit(post, true, null);
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
        notifyAudit(post, false, reason);
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

    private void notifyAudit(Post post, boolean approved, String reason) {
        try {
            String state = approved ? "approved" : "rejected";
            auditGateway.notify("community-post-manual-audit-" + post.getId() + "-" + state,
                    post.getUserId(), approved ? "游记审核通过" : "游记审核未通过",
                    approved
                            ? "《" + post.getTitle() + "》已通过人工审核并发布。"
                            : "《" + post.getTitle() + "》未通过人工审核，原因：" + reason,
                    "/post/" + post.getId());
        } catch (Exception exception) {
            log.warn("人工审核结果已写入，但通知发送失败。postId={}, error={}", post.getId(), exception.getMessage());
        }
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
