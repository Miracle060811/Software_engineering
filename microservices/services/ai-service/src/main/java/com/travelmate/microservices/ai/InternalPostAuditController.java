package com.travelmate.microservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/ai")
public class InternalPostAuditController {
    private final AiPostAuditService service;
    private final String token;

    public InternalPostAuditController(AiPostAuditService service,
                                       @Value("${app.internal-service-token}") String token) {
        this.service = service;
        this.token = token;
    }

    @PostMapping("/post-audit")
    public AiPostAuditService.AuditDecision audit(@RequestBody AiPostAuditService.AuditRequest request,
                                                  @RequestHeader("X-Internal-Token") String supplied) {
        if (!token.equals(supplied)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        return service.audit(request);
    }
}
