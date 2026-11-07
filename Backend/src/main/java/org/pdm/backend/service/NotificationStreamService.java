package org.pdm.backend.service;

import org.pdm.backend.model.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationStreamService {
    /**
     * Subscribes a user to a Server-Sent Events (SSE) stream for real-time notifications.
     *
     * @param userId The ID of the user subscribing to the stream.
     * @return An SseEmitter used to send real-time updates to the client.
     */
    SseEmitter subscribe(Long userId);

    /**
     * Publishes a new notification to the subscribed user's stream.
     *
     * @param notification The notification to be published.
     */
    void publishCreated(Notification notification);

    /**
     * Notifies the user that their notification list has changed and should be refreshed.
     *
     * @param userId The ID of the user whose stream should be notified.
     */
    void publishChanged(Long userId);
}
