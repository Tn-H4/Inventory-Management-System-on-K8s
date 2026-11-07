package org.pdm.backend.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class Product {


    private Long id;

    @NotBlank(message = "Name is required")
    private String name;


    private String sku;

    @Positive(message = "product price must be a positive value")
    private BigDecimal price;

    @Min(value = 0, message = "stock quantity cannot be negative")
    private Integer stockQuantity;

    @Builder.Default
    private Integer reorderThreshold = 10;

    private Long warehouseId;

    private String description;

    @Builder.Default
    private LocalDateTime expiryDate= LocalDateTime.now().plusMonths(12);

    private String imageUrl;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean isDeleted = false;

    private Long categoryId;

    private Long createdBy;

    private Long updatedBy;


    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", description='" + description + '\'' +
                ", expiryDate=" + expiryDate +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public void setProductId(Long productId) {
    }
}
