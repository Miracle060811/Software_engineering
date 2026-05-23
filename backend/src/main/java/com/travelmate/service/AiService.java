package com.travelmate.service;

import com.travelmate.dto.AiChatDTO;
import com.travelmate.dto.AiPlanCreateDTO;
import com.travelmate.dto.PostAuditResult;
import com.travelmate.entity.AiChat;
import com.travelmate.entity.AiPlan;
import com.travelmate.entity.Notification;

import java.util.List;

public interface AiService {

    AiPlan generatePlan(AiPlanCreateDTO dto, Long userId);

    List<AiPlan> listMyPlans(Long userId);

    AiPlan getPlanById(Long id, Long userId);

    AiChat chat(AiChatDTO dto, Long userId);

    PostAuditResult auditPost(String title, String content, String tags, String destination);

    List<Notification> listNotifications(Long userId);

    void markRead(Long id, Long userId);

    void deleteNotification(Long id, Long userId);

    void deleteAllNotifications(Long userId);

    long unreadCount(Long userId);
}
