package com.example.community.dto.notification;

public record MessageNotificationEvent(
    String type,
    String sender,
    String title,
    Long messageId,
    long unreadCount
) {}

