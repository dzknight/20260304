package com.example.community.dto;

public record LikeResponse(
    Long postId,
    long likeCount,
    boolean liked
) {}

