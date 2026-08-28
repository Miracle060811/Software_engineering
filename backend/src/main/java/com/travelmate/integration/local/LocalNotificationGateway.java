package com.travelmate.integration.local;

import com.travelmate.integration.NotificationGateway;
import com.travelmate.service.NotificationCenterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.integration.mode", havingValue = "local", matchIfMissing = true)
public class LocalNotificationGateway implements NotificationGateway {
    private final NotificationCenterService notificationCenterService;

    public LocalNotificationGateway(NotificationCenterService notificationCenterService) {
        this.notificationCenterService = notificationCenterService;
    }

    @Override
    public void publish(Long userId, String type, String title, String content, String link) {
        notificationCenterService.createNotification(userId, type, title, content, link);
    }
}
