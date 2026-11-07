package org.pdm.backend.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Transaction {
    private Long id;

    private Integer totalProducts;

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // purchase, sale, return

    @Enumerated(EnumType.STRING)
    private TransactionStatus status; //pending, completed, processing

    private String description;
    private String note;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;


    private Long productId;
    private Product product;

    private Long userId;
    private User user;

    private Long supplierId;
    private Supplier supplier;

    private Long createdBy;
    private Long updatedBy;


    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", totalProducts=" + totalProducts +
                ", totalPrice=" + totalPrice +
                ", transactionType=" + transactionType +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }


}
