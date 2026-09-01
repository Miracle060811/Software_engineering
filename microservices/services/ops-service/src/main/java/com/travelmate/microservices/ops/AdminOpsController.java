package com.travelmate.microservices.ops;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.entity.SysSensitiveWord;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOpsController {
    private final OpsAggregationGateway gateway;
    private final OpsLocalService localService;
    private final UserContext userContext;

    public AdminOpsController(OpsAggregationGateway gateway, OpsLocalService localService, UserContext userContext) {
        this.gateway = gateway;
        this.localService = localService;
        this.userContext = userContext;
    }

    @GetMapping("/stats") public Result<Map<String, Object>> stats() { return Result.success(gateway.stats()); }
    @GetMapping("/users") public Result<List<Map<String, Object>>> users() { return Result.success(gateway.users()); }
    @GetMapping("/orders") public Result<List<Map<String, Object>>> orders() { return Result.success(gateway.orders()); }
    @GetMapping("/flights") public Result<List<Map<String, Object>>> flights() { return Result.success(gateway.flights()); }
    @GetMapping("/posts") public Result<List<Map<String, Object>>> posts() { return Result.success(gateway.posts()); }

    @PostMapping("/posts/{id}/approve")
    public Result<Map<String, Object>> approvePost(@PathVariable Long id) {
        Map<String, Object> result = gateway.approvePost(id);
        localService.log(userContext.getCurrentUserId(), "审核通过游记: " + id, 1, null);
        return Result.success(result);
    }

    @GetMapping("/review-reports")
    public Result<List<Map<String, Object>>> reviewReports(@RequestParam(required = false) Integer status) {
        return Result.success(gateway.reviewReports(status));
    }

    @PostMapping("/review-reports/{id}/resolve")
    public Result<Map<String, Object>> resolveReport(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = gateway.resolveReport(id, body);
        localService.log(userContext.getCurrentUserId(), "处理评价举报: " + id, 1, null);
        return Result.success(result);
    }

    @GetMapping("/sensitive-words")
    public Result<List<SysSensitiveWord>> sensitiveWords() { return Result.success(localService.listSensitiveWords()); }

    @PostMapping("/sensitive-words")
    public Result<SysSensitiveWord> addSensitiveWord(@RequestBody Map<String, Object> body) {
        Integer level = body.get("level") == null ? null : Integer.valueOf(body.get("level").toString());
        return Result.success(localService.addSensitiveWord((String) body.get("word"), level, userContext.getCurrentUserId()));
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        localService.deleteSensitiveWord(id, userContext.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<Map<String, Object>> logs(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return Result.success(localService.logs(page, size));
    }
}
