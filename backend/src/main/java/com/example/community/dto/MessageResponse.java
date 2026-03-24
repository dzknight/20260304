package com.example.community.dto;

import com.example.community.domain.Message;

import java.time.LocalDateTime;

public record MessageResponse(
    Long id,
    String sender,
    String receiver,
    String title,
    String content,
    boolean read,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getSender().getUsername(),
            m.getReceiver().getUsername(),
            m.getTitle(),
            m.getContent(),
            m.isRead(),
            m.getReadAt(),
            m.getCreatedAt()
        );
    }
}

