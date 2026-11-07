package org.pdm.backend.wrappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pdm.backend.enums.TransactionType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "product id is required")
    private Long productId;

    @Positive(message = "quantity id is required")
    private Integer quantity;

    private Long supplierId;

    private String description;

    private String note;

    private TransactionType transactionType;


}