package com.example.community.dto;

import com.example.community.domain.Post;

import java.time.LocalDateTime;

public record PostItemResponse(
    Long id,
    String title,
    String content,
    String author,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    long likeCount,
    long commentCount,
    boolean likedByMe
) {
    public static PostItemResponse of(Post post, long likeCount, long commentCount, boolean likedByMe) {
        return new PostItemResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getAuthor().getUsername(),
            post.getCreatedAt(),
            post.getUpdatedAt(),
            likeCount,
            commentCount,
            likedByMe
        );
    }
}

