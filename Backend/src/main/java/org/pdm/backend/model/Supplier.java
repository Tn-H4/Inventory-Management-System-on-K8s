package org.pdm.backend.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pdm.backend.enums.SupplierStatus;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Supplier {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String address;

    @NotBlank(message = "Contact info is required")
    private String contactInfo;

    private String email;
    private String website;
    private String taxCode;

    @Builder.Default
    private SupplierStatus status = SupplierStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
