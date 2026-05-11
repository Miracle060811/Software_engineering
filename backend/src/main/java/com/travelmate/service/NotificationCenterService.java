package com.travelmate.service;

import com.travelmate.entity.Notification;

import java.util.List;

public interface NotificationCenterService {

    void createNotification(Long userId, String type, String title, String content);

    List<Notification> listNotifications(Long userId);

    void markRead(Long id, Long userId);

    long unreadCount(Long userId);
}