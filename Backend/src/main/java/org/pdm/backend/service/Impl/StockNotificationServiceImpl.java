package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.model.Product;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.NotificationRepository;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.service.NotificationService;
import org.pdm.backend.service.NotificationStreamService;
import org.pdm.backend.service.StockNotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockNotificationServiceImpl implements StockNotificationService {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Checks the stock level of a product and sends notifications to active users (ADMIN, MANAGER, STAFF)
     * if the stock is low or out of stock.
     *
     * @param product The product to check.
     */
    @Override
    public void checkAndNotify(Product product) {
        checkAndNotify(product, null);
    }

    /**
     * Checks the stock level of a product and sends notifications to active users (ADMIN, MANAGER, STAFF)
     * if the stock is low or out of stock, comparing the current state with a previous stock quantity.
     *
     * @param product The product to check.
     * @param previousStockQuantity The previous stock quantity (optional, used to avoid duplicate notifications for the same state).
     */
    @Override
    public void checkAndNotify(Product product, Integer previousStockQuantity) {
        if (product == null || product.getId() == null) {
            return;
        }

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 10;
        String currentState = getStockState(stock, threshold);
        String previousState = previousStockQuantity == null ? "UNKNOWN" : getStockState(previousStockQuantity, threshold);

        if ("NORMAL".equals(currentState) || currentState.equals(previousState)) {
            return;
        }

        NotificationType type;
        String statusLabel;
        String eventType;

        if ("OUT".equals(currentState)) {
            type = NotificationType.ERROR;
            statusLabel = "Out of stock";
            eventType = "STOCK_OUT";
        } else {
            type = NotificationType.WARNING;
            statusLabel = "Low stock";
            eventType = "STOCK_LOW";
        }

        String productName = product.getName() != null ? product.getName() : "Product";
        String sku = product.getSku() != null ? product.getSku() : "N/A";
        String title = statusLabel + ": " + productName;
        String message = String.format(
                "SKU %s has %d unit(s) on hand (reorder threshold: %d).",
                sku,
                stock,
                threshold
        );
        String dedupeKey = "stock:" + product.getId() + ":" + currentState;

        List<User> recipients = userRepository.findAllActive().stream()
                .filter(user -> user.getRole() == UserRole.ADMIN
                        || user.getRole() == UserRole.MANAGER
                        || user.getRole() == UserRole.STAFF)
                .toList();

        for (User user : recipients) {
            if (user.getId() == null) {
                continue;
            }
            try {
                var existingNotification = notificationRepository.findUnreadByUserIdAndDedupeKey(user.getId(), dedupeKey);
                if (existingNotification.isPresent() && !existingNotification.get().getIsRead()) {
                    // Update existing unread notification with new message and timestamp
                    notificationRepository.updateNotification(
                            existingNotification.get().getId(),
                            title,
                            message,
                            type,
                            java.time.LocalDateTime.now()
                    );
                    notificationStreamService.publishChanged(user.getId());
                } else {
                    String actionUrl = user.getRole() == UserRole.STAFF
                            ? "/dashboard"
                            : "/products/" + product.getId() + "/edit";
                    // Create new notification
                    notificationService.createNotification(
                            user.getId(),
                            title,
                            message,
                            type,
                            eventType,
                            "PRODUCT",
                            product.getId(),
                            actionUrl,
                            dedupeKey
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to create stock notification for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    private String getStockState(int stock, int threshold) {
        if (stock == 0) {
            return "OUT";
        }
        if (stock < threshold) {
            return "LOW";
        }
        return "NORMAL";
    }
}
