package org.pdm.backend.service;

import org.pdm.backend.model.Product;

public interface StockNotificationService {
    /**
     * Creates unread WARNING/ERROR notifications for all ADMIN and MANAGER users
     * when the product is low or out of stock. Skips when stock is adequate.
     *
     * @param product The product to check for stock levels.
     */
    void checkAndNotify(Product product);

    /**
     * Creates unread WARNING/ERROR notifications for all ADMIN and MANAGER users
     * when the product is low or out of stock, comparing with the previous stock quantity.
     *
     * @param product The product to check for stock levels.
     * @param previousStockQuantity The stock quantity before the current operation.
     */
    void checkAndNotify(Product product, Integer previousStockQuantity);
}
