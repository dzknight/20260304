package com.example.community.controller;

import com.example.community.dto.MessageRequest;
import com.example.community.dto.MessageResponse;
import com.example.community.security.JwtUtil;
import com.example.community.service.MessageService;
import com.example.community.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/send")
    public MessageResponse send(@Valid @RequestBody MessageRequest req, Authentication auth) {
        return messageService.send(auth.getName(), req);
    }

    @GetMapping("/received")
    public Page<MessageResponse> received(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "onlyUnread", defaultValue = "false") boolean onlyUnread,
        Authentication auth
    ) {
        return messageService.received(auth.getName(), onlyUnread, page, size);
    }

    @GetMapping("/sent")
    public Page<MessageResponse> sent(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        Authentication auth
    ) {
        return messageService.sent(auth.getName(), page, size);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication auth) {
        return messageService.unreadCount(auth.getName());
    }

    @PostMapping("/{id}/read")
    public MessageResponse read(@PathVariable Long id, Authentication auth) {
        return messageService.read(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        messageService.delete(auth.getName(), id);
    }

    @GetMapping("/blocks")
    public List<String> blockedUsers(Authentication auth) {
        return messageService.blockedUsers(auth.getName());
    }

    @PostMapping("/block/{username}")
    public void block(@PathVariable String username, Authentication auth) {
        messageService.blockUser(auth.getName(), username);
    }

    @DeleteMapping("/block/{username}")
    public void unblock(@PathVariable String username, Authentication auth) {
        messageService.unblockUser(auth.getName(), username);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            SseEmitter emitter = notificationService.subscribe(username);
            long unread = messageService.unreadCount(username);
            notificationService.sendUnreadCount(username, unread);
            return emitter;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}
