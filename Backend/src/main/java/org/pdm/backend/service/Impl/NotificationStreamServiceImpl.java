package org.pdm.backend.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.model.Notification;
import org.pdm.backend.service.NotificationStreamService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationStreamServiceImpl implements NotificationStreamService {

    private static final long TIMEOUT_MS = 30L * 60L * 1000L;
    private static final long HEARTBEAT_INTERVAL_MS = 20_000;
    private static final String KEEP_ALIVE_COMMENT = "keep-alive";

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    /**
     * Subscribes a user to a Server-Sent Events (SSE) stream for real-time notifications.
     *
     * @param userId The ID of the user subscribing.
     * @return An SseEmitter for the notification stream.
     */
    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUserId.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification.connected")
                        .data(Map.of("status", "connected")));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(userId, emitter);
            }
        }

        return emitter;
    }

    /**
     * Publishes a "created" event to the user's notification stream.
     *
     * @param notification The created notification.
     */
    @Override
    public void publishCreated(Notification notification) {
        if (notification == null || notification.getUserId() == null) {
            return;
        }
        publish(notification.getUserId(), "notification.created", notification);
    }

    /**
     * Publishes a "changed" event to the user's notification stream (e.g., when a notification is marked as read).
     *
     * @param userId The ID of the user whose notifications changed.
     */
    @Override
    public void publishChanged(Long userId) {
        if (userId == null) {
            return;
        }
        publish(userId, "notification.changed", Map.of("userId", userId));
    }

    private void publish(Long userId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            synchronized (emitter) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(payload));
                } catch (IOException | IllegalStateException e) {
                    log.debug("Removing closed notification stream for user {}", userId);
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    /**
     * Sends a heartbeat comment to all active SSE streams to keep them open and detect disconnects.
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                synchronized (emitter) {
                    try {
                        emitter.send(SseEmitter.event().comment(KEEP_ALIVE_COMMENT));
                    } catch (IOException | IllegalStateException e) {
                        log.debug("Removing closed notification stream for user {} during heartbeat", userId);
                        removeEmitter(userId, emitter);
                    }
                }
            }
        });
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        emittersByUserId.computeIfPresent(userId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
