package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.PrivateMessage;
import com.travelmate.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/private-message")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @Autowired
    private UserContext userContext;

    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> contacts() {
        return Result.success(privateMessageService.listContacts(userContext.getCurrentUserId()));
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> searchUsers(@RequestParam String keyword) {
        return Result.success(privateMessageService.searchUsers(userContext.getCurrentUserId(), keyword));
    }

    @GetMapping("/conversation/{userId}")
    public Result<List<PrivateMessage>> conversation(@PathVariable Long userId) {
        return Result.success(privateMessageService.listConversation(userContext.getCurrentUserId(), userId));
    }

    @PostMapping("/send")
    public Result<PrivateMessage> send(@RequestBody PrivateMessageSendDTO dto) {
        return Result.success(privateMessageService.sendMessage(userContext.getCurrentUserId(), dto));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        return Result.success(privateMessageService.unreadCount(userContext.getCurrentUserId()));
    }
}
