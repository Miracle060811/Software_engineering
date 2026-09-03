package com.travelmate.service.impl;

import com.travelmate.entity.AiPlan;
import com.travelmate.entity.Notification;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiNotificationNavigationTests {

    @Test
    void buildsDirectActionUrlForNewPlan() {
        assertEquals("/ai-plan?planId=42", AiServiceImpl.buildAiPlanActionUrl(42L));
    }

    @Test
    void enrichesLegacyNotificationsWithMatchingPlanIds() {
        AiServiceImpl service = new AiServiceImpl();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 25, 15, 30);

        Notification hangzhouNotice = notification(
                1L,
                "您的 杭州 3 天行程已生成，可在行程列表中查看详情。",
                createdAt.plusSeconds(1));
        Notification chengduNotice = notification(
                2L,
                "您的 成都 5 天行程已生成，可在行程列表中查看详情。",
                createdAt.plusMinutes(2));
        Notification alreadyTargeted = notification(3L, "已有新行程", createdAt.plusMinutes(3));
        alreadyTargeted.setActionUrl("/ai-plan?planId=99");

        List<Notification> notifications = new ArrayList<>(List.of(
                alreadyTargeted,
                chengduNotice,
                hangzhouNotice));
        List<AiPlan> plans = List.of(
                plan(22L, "成都", 5, createdAt.plusMinutes(2).minusSeconds(1)),
                plan(11L, "杭州", 3, createdAt));

        service.enrichLegacyAiPlanActionUrls(notifications, plans);

        assertEquals("/ai-plan?planId=99", alreadyTargeted.getActionUrl());
        assertEquals("/ai-plan?planId=22", chengduNotice.getActionUrl());
        assertEquals("/ai-plan?planId=11", hangzhouNotice.getActionUrl());
    }

    private Notification notification(Long id, String content, LocalDateTime createTime) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType("ai_plan");
        notification.setContent(content);
        notification.setActionUrl("/ai-plan");
        notification.setCreateTime(createTime);
        return notification;
    }

    private AiPlan plan(Long id, String destination, int days, LocalDateTime createTime) {
        AiPlan plan = new AiPlan();
        plan.setId(id);
        plan.setDestination(destination);
        plan.setDays(days);
        plan.setCreateTime(createTime);
        return plan;
    }
}
