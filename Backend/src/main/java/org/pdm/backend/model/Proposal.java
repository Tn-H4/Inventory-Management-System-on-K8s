package org.pdm.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pdm.backend.enums.ProposalStatus;
import org.pdm.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {
    private Long id;
    private TransactionType transactionType;
    private ProposalStatus status;
    private Long productId;
    private Long supplierId;
    private Integer quantity;
    private String description;
    private BigDecimal totalPrice;
    private Long proposedBy;
    private Long reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private Long transactionId;

    // For display purposes - optional nested objects
    private Product product;
    private Supplier supplier;
    private User proposedByUser;
    private User reviewedByUser;
}