package com.travelmate.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelmate.dto.PostAuditResult;
import com.travelmate.entity.Post;
import com.travelmate.mapper.PostMapper;
import com.travelmate.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private AiService aiService;

    @Scheduled(fixedRate = 60 * 1000)
    public void auditPendingPosts() {
        if (!RUNNING.compareAndSet(false, true)) {
            log.info("上一轮游记 AI 审核仍在执行，本轮跳过");
            return;
        }
        try {
            List<Post> pendingPosts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 0)
                    .orderByAsc(Post::getCreateTime)
                    .last("LIMIT " + MAX_AUDIT_PER_MINUTE));
            if (pendingPosts.isEmpty()) {
                return;
            }
            log.info("开始处理游记 AI 审核队列，本轮最多 {} 条，实际 {} 条",
                    MAX_AUDIT_PER_MINUTE, pendingPosts.size());
            for (Post post : pendingPosts) {
                auditOne(post);
            }
        } finally {
            RUNNING.set(false);
        }
    }

    private void auditOne(Post post) {
        try {
            PostAuditResult auditResult = aiService.auditPost(
                    post.getTitle(),
                    post.getContent(),
                    post.getTags(),
                    post.getDestination());
            boolean approved = auditResult == null || auditResult.isApproved();
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, post.getId())
                    .eq(Post::getStatus, 0)
                    .set(Post::getStatus, approved ? 1 : 2)
                    .set(Post::getRejectReason, approved ? null : auditResult.getReason())
                    .set(Post::getUpdateTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("游记 AI 审核失败，保留在队列等待下次处理。postId={}, error={}", post.getId(), e.getMessage());
        }
    }
}
