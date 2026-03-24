package com.example.community.dto;

import com.example.community.domain.OperationLog;

import java.time.LocalDateTime;

public record OperationLogResponse(
    Long id,
    String actor,
    String action,
    String targetType,
    Long targetId,
    String targetRef,
    String detail,
    LocalDateTime createdAt
) {
    public static OperationLogResponse from(OperationLog log) {
        return new OperationLogResponse(
            log.getId(),
            log.getActor(),
            log.getAction(),
            log.getTargetType(),
            log.getTargetId(),
            log.getTargetRef(),
            log.getDetail(),
            log.getCreatedAt()
        );
    }
}
