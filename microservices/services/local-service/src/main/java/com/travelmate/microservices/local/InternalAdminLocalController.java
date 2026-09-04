package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.ReviewReport;
import com.travelmate.entity.Review;
import com.travelmate.entity.Reply;
import com.travelmate.mapper.ReviewMapper;
import com.travelmate.mapper.ReplyMapper;
import com.travelmate.mapper.ReviewReportMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/local/admin")
public class InternalAdminLocalController {
    private final ReviewReportMapper mapper;
    private final ReviewMapper reviewMapper;
    private final ReplyMapper replyMapper;
    private final String token;

    public InternalAdminLocalController(ReviewReportMapper mapper, ReviewMapper reviewMapper, ReplyMapper replyMapper,
                                        @Value("${app.internal-service-token}") String token) {
        this.mapper = mapper;
        this.reviewMapper = reviewMapper;
        this.replyMapper = replyMapper;
        this.token = token;
    }

    @GetMapping("/review-reports")
    public List<ReviewReport> reports(@RequestParam(required = false) Integer status,
                                      @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        LambdaQueryWrapper<ReviewReport> query = new LambdaQueryWrapper<ReviewReport>()
                .orderByDesc(ReviewReport::getCreateTime);
        if (status != null) query.eq(ReviewReport::getStatus, status);
        return mapper.selectList(query);
    }

    @PostMapping("/review-reports/{id}/resolve")
    public ReviewReport resolve(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        ReviewReport report = mapper.selectById(id);
        if (report == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价举报不存在");
        report.setStatus(1);
        report.setHandleRemark(body.get("remark") == null ? null : body.get("remark").toString().trim());
        report.setHandleTime(LocalDateTime.now());
        mapper.updateById(report);
        return report;
    }

    @PostMapping("/review-reports/{id}/reject")
    public ReviewReport reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                               @RequestHeader("X-Internal-Token") String supplied) {
        return closeReport(id, body, "举报不成立，已驳回", supplied);
    }

    @PostMapping("/review-reports/{id}/delete-review")
    public ReviewReport deleteReportedReview(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, Object> body,
                                             @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        ReviewReport report = requiredReport(id);
        Review review = reviewMapper.selectById(report.getReviewId());
        if (review == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价不存在");
        review.setDeleted(1);
        reviewMapper.updateById(review);
        return closeReport(report, body, "举报成立，评价已删除");
    }

    @GetMapping("/reviews/{reviewId}/replies")
    public List<Reply> replies(@PathVariable Long reviewId,
                               @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return replyMapper.selectList(new LambdaQueryWrapper<Reply>()
                .eq(Reply::getReviewId, reviewId).eq(Reply::getDeleted, 0)
                .orderByAsc(Reply::getCreateTime));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public Reply addReply(@PathVariable Long reviewId, @RequestBody Map<String, Object> body,
                          @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        String content = body.get("content") == null ? "" : body.get("content").toString().trim();
        if (content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        Object adminId = body.get("adminId");
        if (adminId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员编号不能为空");
        Reply reply = new Reply();
        reply.setReviewId(reviewId);
        reply.setUserId(Long.valueOf(adminId.toString()));
        reply.setContent(content);
        reply.setDeleted(0);
        reply.setCreateTime(LocalDateTime.now());
        replyMapper.insert(reply);
        return reply;
    }

    @DeleteMapping("/replies/{id}")
    public void deleteReply(@PathVariable Long id, @RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        Reply reply = replyMapper.selectById(id);
        if (reply == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "回复不存在");
        reply.setDeleted(1);
        replyMapper.updateById(reply);
    }

    @GetMapping("/pending-report-count")
    public long pendingCount(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectCount(new LambdaQueryWrapper<ReviewReport>().eq(ReviewReport::getStatus, 0));
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }

    private ReviewReport requiredReport(Long id) {
        ReviewReport report = mapper.selectById(id);
        if (report == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价举报不存在");
        return report;
    }

    private ReviewReport closeReport(Long id, Map<String, Object> body, String fallback, String supplied) {
        verify(supplied);
        return closeReport(requiredReport(id), body, fallback);
    }

    private ReviewReport closeReport(ReviewReport report, Map<String, Object> body, String fallback) {
        String remark = body == null || body.get("remark") == null ? fallback : body.get("remark").toString().trim();
        report.setStatus(1);
        report.setHandleRemark(remark.isBlank() ? fallback : remark);
        report.setHandleTime(LocalDateTime.now());
        mapper.updateById(report);
        return report;
    }
}
