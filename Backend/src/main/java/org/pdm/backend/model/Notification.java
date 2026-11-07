package org.pdm.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pdm.backend.enums.NotificationType;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Notification {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private String eventType;
    private String entityType;
    private Long entityId;
    private String actionUrl;
    private String dedupeKey;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
