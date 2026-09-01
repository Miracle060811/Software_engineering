package com.travelmate.microservices.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/ops/content")
public class InternalContentSafetyController {
    private final OpsLocalService localService;
    private final String token;

    public InternalContentSafetyController(OpsLocalService localService,
                                           @Value("${app.internal-service-token}") String token) {
        this.localService = localService;
        this.token = token;
    }

    @PostMapping("/check")
    public ContentCheck check(@RequestBody ContentRequest request,
                              @RequestHeader("X-Internal-Token") String suppliedToken) {
        if (!token.equals(suppliedToken)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        return new ContentCheck(localService.containsSensitiveWord(request.content()));
    }

    public record ContentRequest(String content) {}
    public record ContentCheck(boolean sensitive) {}
}
