package com.example.community.service;

import com.example.community.domain.Member;
import com.example.community.domain.Message;
import com.example.community.domain.MessageBlock;
import com.example.community.dto.MessageRequest;
import com.example.community.dto.MessageResponse;
import com.example.community.repository.MessageBlockRepository;
import com.example.community.repository.MemberRepository;
import com.example.community.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageBlockRepository messageBlockRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    @Transactional
    public MessageResponse send(String senderUsername, MessageRequest req) {
        Member sender = memberRepository.findByUsername(senderUsername)
            .orElseThrow(() -> new IllegalArgumentException("보내는 회원 없음"));
        Member receiver = memberRepository.findByUsername(req.receiverUsername())
            .orElseThrow(() -> new IllegalArgumentException("받는 회원 없음"));
        if (messageBlockRepository.existsByOwnerAndBlockedMember(receiver, sender)) {
            throw new IllegalStateException("해당 회원이 발신인을 차단했습니다.");
        }

        Message message = Message.builder()
            .sender(sender)
            .receiver(receiver)
            .title(req.title())
            .content(req.content())
            .build();
        Message saved = messageRepository.save(message);
        long unreadCount = messageRepository.countByReceiverAndReadFalse(receiver);
        notificationService.sendNewMessage(saved, unreadCount);
        return MessageResponse.from(saved);
    }

    public Page<MessageResponse> received(String username, boolean onlyUnread, int page, int size) {
        Member receiver = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Pageable pageable = pageRequest(page, size);
        Page<Message> messages = onlyUnread
            ? messageRepository.findByReceiverAndReadFalseOrderByCreatedAtDesc(receiver, pageable)
            : messageRepository.findByReceiverOrderByCreatedAtDesc(receiver, pageable);
        return messages.map(MessageResponse::from);
    }

    public Page<MessageResponse> sent(String username, int page, int size) {
        Member sender = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Pageable pageable = pageRequest(page, size);
        return messageRepository.findBySenderOrderByCreatedAtDesc(sender, pageable).map(MessageResponse::from);
    }

    @Transactional
    public MessageResponse read(String username, Long id) {
        Member receiver = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("쪽지 없음"));
        if (!message.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalStateException("읽기 권한이 없습니다.");
        }
        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
        }
        notificationService.sendUnreadCount(username, messageRepository.countByReceiverAndReadFalse(receiver));
        return MessageResponse.from(message);
    }

    @Transactional
    public void delete(String username, Long id) {
        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("쪽지 없음"));
        if (!message.getReceiver().getUsername().equals(username) &&
            !message.getSender().getUsername().equals(username)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        boolean isReceiver = message.getReceiver().getUsername().equals(username);
        boolean unread = !message.isRead();
        String counterpart = isReceiver ? message.getSender().getUsername() : message.getReceiver().getUsername();
        messageRepository.delete(message);
        operationLogService.record(
            username,
            OperationLogService.ACTION_DELETE_MESSAGE,
            "MESSAGE",
            id,
            message.getTitle(),
            String.format("쪽지 삭제: 상대방=%s", counterpart)
        );
        if (isReceiver && unread) {
            notificationService.sendUnreadCount(username, messageRepository.countByReceiverAndReadFalse(message.getReceiver()));
        }
    }

    @Transactional
    public void blockUser(String ownerUsername, String blockedUsername) {
        if (ownerUsername.equals(blockedUsername)) {
            throw new IllegalStateException("자기 자신은 차단할 수 없습니다.");
        }

        Member owner = memberRepository.findByUsername(ownerUsername)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Member blockedMember = memberRepository.findByUsername(blockedUsername)
            .orElseThrow(() -> new IllegalArgumentException("대상 회원이 없습니다."));

        boolean alreadyBlocked = messageBlockRepository.existsByOwnerAndBlockedMember(owner, blockedMember);
        if (alreadyBlocked) {
            return;
        }

        messageBlockRepository.save(MessageBlock.builder()
            .owner(owner)
            .blockedMember(blockedMember)
            .build());
        operationLogService.record(
            ownerUsername,
            OperationLogService.ACTION_BLOCK_USER,
            "USER",
            blockedMember.getId(),
            blockedMember.getUsername(),
            String.format("차단 대상: %s", blockedUsername)
        );
    }

    @Transactional
    public void unblockUser(String ownerUsername, String blockedUsername) {
        Member owner = memberRepository.findByUsername(ownerUsername)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Member blockedMember = memberRepository.findByUsername(blockedUsername)
            .orElseThrow(() -> new IllegalArgumentException("대상 회원이 없습니다."));
        messageBlockRepository.deleteByOwnerAndBlockedMember(owner, blockedMember);
        operationLogService.record(
            ownerUsername,
            OperationLogService.ACTION_UNBLOCK_USER,
            "USER",
            blockedMember.getId(),
            blockedMember.getUsername(),
            String.format("차단 해제 대상: %s", blockedUsername)
        );
    }

    public List<String> blockedUsers(String ownerUsername) {
        Member owner = memberRepository.findByUsername(ownerUsername)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        return messageBlockRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
            .map(block -> block.getBlockedMember().getUsername())
            .collect(Collectors.toList());
    }

    public long unreadCount(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        return messageRepository.countByReceiverAndReadFalse(member);
    }

    private Pageable pageRequest(int page, int size) {
        int pageNo = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size <= 0 ? 10 : size, 50));
        return PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
