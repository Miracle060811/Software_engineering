package com.travelmate.integration;

public interface NotificationGateway {
    void publish(Long userId, String type, String title, String content, String link);
}
