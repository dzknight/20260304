package com.example.community.dto;

public record AuthResponse(
    Long id,
    String username,
    String email,
    String nickname,
    String token
) {}

