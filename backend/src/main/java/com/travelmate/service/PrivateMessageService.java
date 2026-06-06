package com.travelmate.service;

import com.travelmate.dto.PrivateMessageSendDTO;
import com.travelmate.entity.PrivateMessage;

import java.util.List;
import java.util.Map;

public interface PrivateMessageService {
    List<Map<String, Object>> listContacts(Long currentUserId);

    List<Map<String, Object>> searchUsers(Long currentUserId, String keyword);

    List<PrivateMessage> listConversation(Long currentUserId, Long contactUserId);

    PrivateMessage sendMessage(Long currentUserId, PrivateMessageSendDTO dto);

    Integer unreadCount(Long currentUserId);
}
