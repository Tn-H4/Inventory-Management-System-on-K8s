package org.pdm.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductSupplier {

    private Long productId;
    private Long supplierId;
    private BigDecimal unitCost;
    private Boolean isPreferred;
    private LocalDateTime createdAt;

    // Tracking columns for supplier-specific return validation
    private Long totalPurchased;
    private Long totalReturned;

    // Joined fields for display purposes
    private String supplierName;
    private String supplierContact;
}
