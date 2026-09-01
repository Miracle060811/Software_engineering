package com.travelmate.microservices.ai;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiPlanChatController {
    private final AiPlanChatService service;
    private final UserContext userContext;

    public AiPlanChatController(AiPlanChatService service, UserContext userContext) {
        this.service = service;
        this.userContext = userContext;
    }

    @PostMapping("/plan/generate")
    public Result<AiPlan> generate(@RequestBody AiPlanCreateDTO dto) {
        return Result.success(service.generate(dto, userContext.getCurrentUserId()));
    }

    @GetMapping("/plan/list")
    public Result<List<AiPlan>> list() { return Result.success(service.list(userContext.getCurrentUserId())); }

    @GetMapping("/plan/{id}")
    public Result<AiPlan> get(@PathVariable Long id) { return Result.success(service.get(id, userContext.getCurrentUserId())); }

    @PostMapping("/chat")
    public Result<AiChat> chat(@RequestBody AiChatDTO dto) { return Result.success(service.chat(dto, userContext.getCurrentUserId())); }
}
