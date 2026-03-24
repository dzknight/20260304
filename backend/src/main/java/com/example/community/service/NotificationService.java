package com.example.community.service;

import com.example.community.domain.Message;
import com.example.community.dto.notification.MessageNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class NotificationService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<SseEmitter>> listeners = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(30 * 60_000L);
        listeners.computeIfAbsent(username, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onError((e) -> removeEmitter(username, emitter));
        return emitter;
    }

    public void sendNewMessage(Message message, long unreadCount) {
        MessageNotificationEvent event = new MessageNotificationEvent(
            "NEW_MESSAGE",
            message.getSender().getUsername(),
            message.getTitle(),
            message.getId(),
            unreadCount
        );
        sendDirect(message.getReceiver().getUsername(), event);
    }

    public void sendUnreadCount(String username, long unreadCount) {
        MessageNotificationEvent event = new MessageNotificationEvent(
            "UNREAD_COUNT",
            "",
            "",
            null,
            unreadCount
        );
        sendDirect(username, event);
    }

    public void sendDirect(String username, MessageNotificationEvent event) {
        Set<SseEmitter> userEmitters = listeners.get(username);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        userEmitters.forEach(emitter -> {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("notification")
                        .data(objectMapper.writeValueAsString(event), MediaType.APPLICATION_JSON)
                );
            } catch (IOException ex) {
                removeEmitter(username, emitter);
            }
        });
    }

    private void removeEmitter(String username, SseEmitter emitter) {
        Set<SseEmitter> list = listeners.get(username);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                listeners.remove(username);
            }
        }
    }
}
