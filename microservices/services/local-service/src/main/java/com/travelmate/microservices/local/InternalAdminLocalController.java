package com.travelmate.microservices.local;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelmate.entity.ReviewReport;
import com.travelmate.mapper.ReviewReportMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final String token;

    public InternalAdminLocalController(ReviewReportMapper mapper, @Value("${app.internal-service-token}") String token) {
        this.mapper = mapper;
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

    @GetMapping("/pending-report-count")
    public long pendingCount(@RequestHeader("X-Internal-Token") String supplied) {
        verify(supplied);
        return mapper.selectCount(new LambdaQueryWrapper<ReviewReport>().eq(ReviewReport::getStatus, 0));
    }

    private void verify(String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
    }
}
