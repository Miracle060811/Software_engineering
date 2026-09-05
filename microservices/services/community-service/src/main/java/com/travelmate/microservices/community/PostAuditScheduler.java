package com.travelmate.microservices.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PostAuditScheduler {
    private static final Logger log = LoggerFactory.getLogger(PostAuditScheduler.class);
    private static final int MAX_AUDIT_PER_MINUTE = 50;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private final PostMapper postMapper;
    private final CommunityPostAuditGateway gateway;

    public PostAuditScheduler(PostMapper postMapper, CommunityPostAuditGateway gateway) {
        this.postMapper = postMapper;
        this.gateway = gateway;
    }

    @Scheduled(fixedRate = 60_000)
    public void auditPendingPosts() {
        if (!RUNNING.compareAndSet(false, true)) return;
        try {
            List<Post> pending = postMapper.selectList(new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0).orderByAsc(Post::getCreateTime)
                    .last("LIMIT " + MAX_AUDIT_PER_MINUTE));
            for (Post post : pending) auditOne(post);
        } finally {
            RUNNING.set(false);
        }
    }

    void auditOne(Post post) {
        try {
            CommunityPostAuditGateway.AuditDecision decision = gateway.audit(
                    post.getTitle(), post.getContent(), post.getTags(), post.getDestination());
            boolean approved = decision == null || decision.approved();
            String reason = approved ? null : decision.reason();
            int updated = postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, post.getId()).eq(Post::getStatus, 0)
                    .set(Post::getStatus, approved ? 1 : 2)
                    .set(Post::getRejectReason, reason)
                    .set(Post::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) return;
            String eventId = "community-post-audit-" + post.getId() + "-" + (approved ? "approved" : "rejected");
            try {
                gateway.notify(eventId, post.getUserId(), approved ? "游记审核通过" : "游记审核未通过",
                        approved
                                ? "《" + post.getTitle() + "》已通过自动审核并发布。"
                                : "《" + post.getTitle() + "》未通过自动审核，原因："
                                        + (reason == null ? "内容不符合社区规范" : reason),
                        "/post/" + post.getId());
            } catch (Exception exception) {
                log.warn("游记审核结果已写入，但通知发送失败。postId={}, error={}", post.getId(), exception.getMessage());
            }
        } catch (Exception exception) {
            log.warn("游记自动审核失败，保留在队列等待下次处理。postId={}, error={}",
                    post.getId(), exception.getMessage());
        }
    }
}
