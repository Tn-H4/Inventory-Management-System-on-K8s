package org.pdm.backend.repository;

import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    /**
     * Saves a new notification.
     *
     * @param notification The notification to save.
     * @return The saved Notification object.
     */
    Notification save(Notification notification);

    /**
     * Retrieves a paginated list of notifications for a user.
     *
     * @param userId The ID of the user.
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of notifications.
     */
    List<Notification> findByUserId(Long userId, int page, int size);

    /**
     * Retrieves a paginated and filtered list of notifications for a user.
     *
     * @param userId The ID of the user.
     * @param page The page number.
     * @param size The number of records per page.
     * @param type The type of notification.
     * @param sortBy The field to sort by.
     * @return A list of notifications.
     */
    List<Notification> findByUserId(Long userId, int page, int size, NotificationType type, String sortBy);

    /**
     * Retrieves all unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A list of unread notifications.
     */
    List<Notification> findByUserIdUnread(Long userId);

    /**
     * Retrieves a notification by its ID.
     *
     * @param id The ID of the notification.
     * @return An Optional containing the notification if found.
     */
    Optional<Notification> findById(Long id);

    /**
     * Counts the total number of unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return The count of unread notifications.
     */
    Long countUnread(Long userId);

    /**
     * Counts the total number of notifications for a user.
     *
     * @param userId The ID of the user.
     * @return The total count.
     */
    Long countByUserId(Long userId);

    /**
     * Counts notifications for a user filtered by type.
     *
     * @param userId The ID of the user.
     * @param type The notification type.
     * @return The total count.
     */
    Long countByUserId(Long userId, NotificationType type);

    /**
     * Marks a specific notification as read.
     *
     * @param id The ID of the notification.
     */
    void markAsRead(Long id);

    /**
     * Marks all notifications for a user as read.
     *
     * @param userId The ID of the user.
     */
    void markAllAsRead(Long userId);

    /**
     * Deletes a notification by its ID.
     *
     * @param id The ID of the notification to delete.
     */
    void deleteById(Long id);

    /**
     * Deletes all notifications for a user.
     *
     * @param userId The ID of the user.
     */
    void deleteAllByUserId(Long userId);

    /**
     * Checks if an unread notification with a specific title exists for a user.
     *
     * @param userId The ID of the user.
     * @param title The title of the notification.
     * @return True if an unread notification with that title exists.
     */
    boolean existsUnreadByUserIdAndTitle(Long userId, String title);

    /**
     * Retrieves a notification by user ID and title.
     *
     * @param userId The ID of the user.
     * @param title The title of the notification.
     * @return An Optional containing the notification if found.
     */
    Optional<Notification> findByUserIdAndTitle(Long userId, String title);

    /**
     * Retrieves an unread notification matching a specific deduplication key for a user.
     *
     * @param userId The ID of the user.
     * @param dedupeKey The deduplication key.
     * @return An Optional containing the notification if found.
     */
    Optional<Notification> findUnreadByUserIdAndDedupeKey(Long userId, String dedupeKey);

    /**
     * Updates the message and timestamp of a notification.
     *
     * @param id The ID of the notification.
     * @param message The new message.
     * @param createdAt The new timestamp.
     */
    void updateNotification(Long id, String message, LocalDateTime createdAt);

    /**
     * Updates the title, message, type, and timestamp of a notification.
     *
     * @param id The ID of the notification.
     * @param title The new title.
     * @param message The new message.
     * @param type The new type.
     * @param createdAt The new timestamp.
     */
    void updateNotification(Long id, String title, String message, NotificationType type, LocalDateTime createdAt);
}
