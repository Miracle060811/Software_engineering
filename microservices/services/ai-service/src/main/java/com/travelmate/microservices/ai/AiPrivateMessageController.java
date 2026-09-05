package com.travelmate.microservices.ai;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.PrivateMessage;
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
@RequestMapping("/api/private-message")
public class AiPrivateMessageController {
    private final AiPrivateMessageService service;
    private final UserContext userContext;

    public AiPrivateMessageController(AiPrivateMessageService service, UserContext userContext) {
        this.service = service;
        this.userContext = userContext;
    }

    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> contacts() {
        return Result.success(service.contacts(userContext.getCurrentUserId()));
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> searchUsers(@RequestParam String keyword) {
        return Result.success(service.searchUsers(userContext.getCurrentUserId(), keyword));
    }

    @PostMapping("/send")
    public Result<PrivateMessage> send(@RequestBody PrivateMessageSendDTO dto) {
        return Result.success(service.send(userContext.getCurrentUserId(), dto));
    }

    @GetMapping("/conversation/{userId}")
    public Result<List<PrivateMessage>> conversation(@PathVariable Long userId) {
        return Result.success(service.conversation(userContext.getCurrentUserId(), userId));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() { return Result.success(service.unreadCount(userContext.getCurrentUserId())); }
}
