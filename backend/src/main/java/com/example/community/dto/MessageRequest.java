package com.example.community.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
    @NotBlank String receiverUsername,
    @NotBlank String title,
    @NotBlank String content
) {}

