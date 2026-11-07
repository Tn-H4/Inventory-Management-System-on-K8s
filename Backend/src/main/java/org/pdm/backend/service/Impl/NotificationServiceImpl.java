package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Notification;
import org.pdm.backend.repository.NotificationRepository;
import org.pdm.backend.service.NotificationService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamServiceImpl notificationStreamService;

    /**
     * Retrieves notifications for a user with basic pagination.
     *
     * @param userId The ID of the user.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @return A Response containing the paginated list of notifications.
     */
    @Override
    public Response getNotifications(Long userId, int page, int size) {
        return getNotifications(userId, page, size, null, "newest");
    }

    /**
     * Retrieves notifications for a user with advanced filtering and sorting.
     *
     * @param userId The ID of the user.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param type The type of notification to filter by.
     * @param sortBy The field to sort by (e.g., "type", "newest").
     * @return A Response containing the paginated list of notifications.
     */
    @Override
    public Response getNotifications(Long userId, int page, int size, String type, String sortBy) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        NotificationType notificationType = parseNotificationType(type);
        String safeSortBy = "type".equalsIgnoreCase(sortBy) ? "type" : "newest";

        List<Notification> notifications = notificationRepository.findByUserId(
                userId,
                safePage,
                safeSize,
                notificationType,
                safeSortBy
        );
        Long totalElements = notificationRepository.countByUserId(userId, notificationType);
        Long unreadCount = notificationRepository.countUnread(userId);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return Response.builder()
                .status(200)
                .message("success")
                .notifications(notifications)
                .unreadCount(unreadCount)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .build();
    }

    /**
     * Retrieves the total number of unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A Response containing the unread count.
     */
    @Override
    public Response getUnreadCount(Long userId) {
        Long count = notificationRepository.countUnread(userId);
        return Response.builder()
                .status(200)
                .message("success")
                .unreadCount(count)
                .build();
    }

    /**
     * Marks a specific notification as read.
     *
     * @param id The ID of the notification.
     * @param userId The ID of the user performing the action.
     * @return A Response indicating the result.
     * @throws NotFoundException if the notification is not found.
     */
    @Override
    public Response markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            return Response.builder()
                    .status(403)
                    .message("You can only mark your own notifications as read")
                    .build();
        }

        notificationRepository.markAsRead(id);
        notificationStreamService.publishChanged(userId);
        return Response.builder()
                .status(200)
                .message("Notification marked as read")
                .build();
    }

    /**
     * Marks all notifications for a user as read.
     *
     * @param userId The ID of the user.
     * @return A Response indicating the result.
     */
    @Override
    public Response markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        notificationStreamService.publishChanged(userId);
        return Response.builder()
                .status(200)
                .message("All notifications marked as read")
                .unreadCount(0L)
                .build();
    }

    /**
     * Deletes a specific notification.
     *
     * @param id The ID of the notification to delete.
     * @param userId The ID of the user performing the action.
     * @return A Response indicating the result.
     * @throws NotFoundException if the notification is not found.
     */
    @Override
    public Response deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            return Response.builder()
                    .status(403)
                    .message("You can only delete your own notifications")
                    .build();
        }

        notificationRepository.deleteById(id);
        notificationStreamService.publishChanged(userId);
        return Response.builder()
                .status(200)
                .message("Notification deleted")
                .build();
    }

    /**
     * Deletes all notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A Response indicating the result.
     */
    @Override
    public Response deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
        notificationStreamService.publishChanged(userId);
        return Response.builder()
                .status(200)
                .message("All notifications deleted")
                .unreadCount(0L)
                .build();
    }

    /**
     * Creates a notification with basic parameters.
     *
     * @param userId The ID of the user to notify.
     * @param title The title of the notification.
     * @param message The message content.
     * @param type The type of the notification.
     * @return The created Notification object.
     */
    @Override
    public Notification createNotification(Long userId, String title, String message, NotificationType type) {
        return createNotification(userId, title, message, type, null, null, null, null, null);
    }

    /**
     * Creates a notification with full details, including event and entity information.
     *
     * @param userId The ID of the user to notify.
     * @param title The title of the notification.
     * @param message The message content.
     * @param type The type of the notification.
     * @param eventType The type of event that triggered the notification.
     * @param entityType The type of entity related to the notification.
     * @param entityId The ID of the related entity.
     * @param actionUrl The URL to navigate to when the notification is clicked.
     * @param dedupeKey A key to prevent duplicate notifications for the same event.
     * @return The created Notification object.
     */
    @Override
    public Notification createNotification(Long userId, String title, String message, NotificationType type,
                                           String eventType, String entityType, Long entityId,
                                           String actionUrl, String dedupeKey) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .actionUrl(actionUrl)
                .dedupeKey(dedupeKey)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        notificationStreamService.publishCreated(saved);
        return saved;
    }

    private NotificationType parseNotificationType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return null;
        }
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
