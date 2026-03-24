package com.example.community.dto;

import com.example.community.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String author,
    String content,
    LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
            c.getId(),
            c.getAuthor().getUsername(),
            c.getContent(),
            c.getCreatedAt()
        );
    }
}

