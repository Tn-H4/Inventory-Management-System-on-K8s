package org.pdm.backend.service;

import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.model.Notification;
import org.pdm.backend.wrappers.Response;

import java.util.List;

public interface NotificationService {
    /**
     * Retrieves a paginated list of notifications for a user.
     *
     * @param userId The ID of the user.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @return A Response containing the paginated list of notifications.
     */
    Response getNotifications(Long userId, int page, int size);

    /**
     * Retrieves a filtered and paginated list of notifications for a user.
     *
     * @param userId The ID of the user.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param type The type of notification to filter by.
     * @param sortBy The field to sort by.
     * @return A Response containing the filtered and paginated list of notifications.
     */
    Response getNotifications(Long userId, int page, int size, String type, String sortBy);

    /**
     * Retrieves the count of unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A Response containing the unread count.
     */
    Response getUnreadCount(Long userId);

    /**
     * Marks a specific notification as read.
     *
     * @param id The ID of the notification.
     * @param userId The ID of the user.
     * @return A Response indicating the result of the operation.
     * @throws org.pdm.backend.exception.NotFoundException if the notification is not found.
     */
    Response markAsRead(Long id, Long userId);

    /**
     * Marks all notifications for a user as read.
     *
     * @param userId The ID of the user.
     * @return A Response indicating the result of the operation.
     */
    Response markAllAsRead(Long userId);

    /**
     * Deletes a specific notification.
     *
     * @param id The ID of the notification.
     * @param userId The ID of the user.
     * @return A Response indicating the result of the operation.
     * @throws org.pdm.backend.exception.NotFoundException if the notification is not found.
     */
    Response deleteNotification(Long id, Long userId);

    /**
     * Deletes all notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A Response indicating the result of the operation.
     */
    Response deleteAllNotifications(Long userId);

    /**
     * Creates a simple notification for a user.
     *
     * @param userId The ID of the user to notify.
     * @param title The title of the notification.
     * @param message The message of the notification.
     * @param type The type of notification.
     * @return The created Notification object.
     */
    Notification createNotification(Long userId, String title, String message, NotificationType type);

    /**
     * Creates a detailed notification for a user, including event and entity metadata.
     *
     * @param userId The ID of the user to notify.
     * @param title The title of the notification.
     * @param message The message of the notification.
     * @param type The type of notification.
     * @param eventType The type of event that triggered the notification.
     * @param entityType The type of entity associated with the notification.
     * @param entityId The ID of the associated entity.
     * @param actionUrl The URL to be used for the action associated with the notification.
     * @param dedupeKey A key used to prevent duplicate notifications for the same event.
     * @return The created Notification object.
     */
    Notification createNotification(Long userId, String title, String message, NotificationType type,
                                    String eventType, String entityType, Long entityId,
                                    String actionUrl, String dedupeKey);
}
