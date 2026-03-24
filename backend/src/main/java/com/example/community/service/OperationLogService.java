package com.example.community.service;

import com.example.community.domain.OperationLog;
import com.example.community.dto.OperationLogResponse;
import com.example.community.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationLogService {
    public static final String ACTION_DELETE_POST = "DELETE_POST";
    public static final String ACTION_DELETE_COMMENT = "DELETE_COMMENT";
    public static final String ACTION_DELETE_MESSAGE = "DELETE_MESSAGE";
    public static final String ACTION_BLOCK_USER = "BLOCK_USER";
    public static final String ACTION_UNBLOCK_USER = "UNBLOCK_USER";

    private final OperationLogRepository repository;

    public Page<OperationLogResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.max(1, Math.min(size <= 0 ? 20 : size, 200)),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return repository.findAllByOrderByCreatedAtDesc(pageable)
            .map(OperationLogResponse::from);
    }

    @Transactional
    public void record(String actor, String action, String targetType, Long targetId, String targetRef, String detail) {
        if (actor == null || actor.isBlank()) {
            return;
        }
        repository.save(OperationLog.builder()
            .actor(actor)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .targetRef(trim(targetRef))
            .detail(trim(detail))
            .build());
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 252) + "...";
    }
}
