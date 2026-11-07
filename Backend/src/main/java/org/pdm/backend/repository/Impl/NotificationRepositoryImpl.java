package org.pdm.backend.repository.Impl;

import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.model.Notification;
import org.pdm.backend.repository.NotificationRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    /**
     * Saves a new notification to the database.
     *
     * @param notification The notification to save.
     * @return The saved Notification object with generated ID.
     */
    @Override
    public Notification save(Notification notification) {
        String sql = """
                INSERT INTO notifications
                    (user_id, title, message, type, event_type, entity_type, entity_id, action_url, dedupe_key, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getType() != null ? notification.getType().name() : NotificationType.INFO.name());
            ps.setString(5, notification.getEventType());
            ps.setString(6, notification.getEntityType());
            if (notification.getEntityId() != null) {
                ps.setLong(7, notification.getEntityId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.setString(8, notification.getActionUrl());
            ps.setString(9, notification.getDedupeKey());
            ps.setBoolean(10, notification.getIsRead() != null ? notification.getIsRead() : false);
            ps.setTimestamp(11, Timestamp.valueOf(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                notification.setId(rs.getLong(1));
            }
            return notification;
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving notification: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves notifications for a user with basic pagination.
     *
     * @param userId The ID of the user.
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of notifications.
     */
    @Override
    public List<Notification> findByUserId(Long userId, int page, int size) {
        return findByUserId(userId, page, size, null, "newest");
    }

    /**
     * Retrieves notifications for a user with advanced filtering and sorting.
     *
     * @param userId The ID of the user.
     * @param page The page number.
     * @param size The number of records per page.
     * @param type The type of notification to filter by.
     * @param sortBy The field to sort by (e.g., "type").
     * @return A list of notifications.
     */
    @Override
    public List<Notification> findByUserId(Long userId, int page, int size, NotificationType type, String sortBy) {
        int offset = (page - 1) * size;
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE user_id = ?");
        if (type != null) {
            sql.append(" AND type = ?");
        }
        if ("type".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY FIELD(type, 'ERROR', 'WARNING', 'SUCCESS', 'INFO'), created_at DESC");
        } else {
            sql.append(" ORDER BY created_at DESC");
        }
        sql.append(" LIMIT ? OFFSET ?");

        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            ps.setLong(paramIndex++, userId);
            if (type != null) {
                ps.setString(paramIndex++, type.name());
            }
            ps.setInt(paramIndex++, size);
            ps.setInt(paramIndex, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifications.add(mapRowToNotification(rs));
            }
            return notifications;
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return A list of unread notifications.
     */
    @Override
    public List<Notification> findByUserIdUnread(Long userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC LIMIT 50";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifications.add(mapRowToNotification(rs));
            }
            return notifications;
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching unread notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a notification by its ID.
     *
     * @param id The ID of the notification.
     * @return An Optional containing the notification if found.
     */
    @Override
    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToNotification(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Database error fetching notification: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the total number of unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return The count of unread notifications.
     */
    @Override
    public Long countUnread(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Counts total notifications for a user.
     *
     * @param userId The ID of the user.
     * @return The total count.
     */
    @Override
    public Long countByUserId(Long userId) {
        return countByUserId(userId, null);
    }

    /**
     * Counts notifications for a user filtered by type.
     *
     * @param userId The ID of the user.
     * @param type The notification type.
     * @return The filtered count.
     */
    @Override
    public Long countByUserId(Long userId, NotificationType type) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM notifications WHERE user_id = ?");
        if (type != null) {
            sql.append(" AND type = ?");
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setLong(1, userId);
            if (type != null) {
                ps.setString(2, type.name());
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Marks a specific notification as read.
     *
     * @param id The ID of the notification.
     */
    @Override
    public void markAsRead(Long id) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error marking notification as read: " + e.getMessage(), e);
        }
    }

    /**
     * Marks all notifications for a user as read.
     *
     * @param userId The ID of the user.
     */
    @Override
    public void markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error marking all notifications as read: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if an unread notification with a specific title exists for a user.
     *
     * @param userId The ID of the user.
     * @param title The title of the notification.
     * @return True if such a notification exists.
     */
    @Override
    public boolean existsUnreadByUserIdAndTitle(Long userId, String title) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND title = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking notification: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a notification by its ID.
     *
     * @param id The ID of the notification to delete.
     */
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting notification: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a notification by user ID and title.
     *
     * @param userId The ID of the user.
     * @param title The title of the notification.
     * @return An Optional containing the notification if found.
     */
    @Override
    public Optional<Notification> findByUserIdAndTitle(Long userId, String title) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND title = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToNotification(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding notification: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an unread notification matching a deduplication key for a user.
     *
     * @param userId The ID of the user.
     * @param dedupeKey The deduplication key.
     * @return An Optional containing the notification if found.
     */
    @Override
    public Optional<Notification> findUnreadByUserIdAndDedupeKey(Long userId, String dedupeKey) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND dedupe_key = ? AND is_read = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, dedupeKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToNotification(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding notification by dedupe key: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all notifications for a specific user.
     *
     * @param userId The ID of the user.
     */
    @Override
    public void deleteAllByUserId(Long userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the message and timestamp of a notification.
     *
     * @param id The ID of the notification.
     * @param message The new message.
     * @param createdAt The new timestamp.
     */
    @Override
    public void updateNotification(Long id, String message, LocalDateTime createdAt) {
        String sql = "UPDATE notifications SET message = ?, created_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setTimestamp(2, Timestamp.valueOf(createdAt));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating notification: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the title, message, type, and timestamp of a notification.
     *
     * @param id The ID of the notification.
     * @param title The new title.
     * @param message The new message.
     * @param type The new type.
     * @param createdAt The new timestamp.
     */
    @Override
    public void updateNotification(Long id, String title, String message, NotificationType type, LocalDateTime createdAt) {
        String sql = "UPDATE notifications SET title = ?, message = ?, type = ?, created_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type != null ? type.name() : NotificationType.INFO.name());
            ps.setTimestamp(4, Timestamp.valueOf(createdAt));
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating notification: " + e.getMessage(), e);
        }
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try {
                n.setType(NotificationType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                // Handle unknown enum values gracefully by defaulting to INFO
                n.setType(NotificationType.INFO);
            }
        }
        n.setEventType(rs.getString("event_type"));
        n.setEntityType(rs.getString("entity_type"));
        long entityId = rs.getLong("entity_id");
        if (!rs.wasNull()) {
            n.setEntityId(entityId);
        }
        n.setActionUrl(rs.getString("action_url"));
        n.setDedupeKey(rs.getString("dedupe_key"));
        n.setIsRead(rs.getBoolean("is_read"));
        if (rs.getTimestamp("created_at") != null) {
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return n;
    }
}
