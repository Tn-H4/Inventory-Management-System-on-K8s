package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.ProductSupplier;
import org.pdm.backend.service.ProductSupplierService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-suppliers")
@RequiredArgsConstructor
@Tag(name = "Product-Supplier Relations", description = "Manage product-supplier relationships")
public class ProductSupplierController {

    private final ProductSupplierService productSupplierService;

    @GetMapping("/all")
    @Operation(summary = "Get all product-supplier links", description = "Retrieves all product-supplier relationships")
    @ApiResponse(responseCode = "200", description = "Links retrieved successfully")
    public ResponseEntity<Response> getAllProductSuppliers() {
        return ResponseEntity.ok(productSupplierService.getAllProductSuppliers());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get suppliers by product", description = "Retrieves all suppliers for a specific product")
    @ApiResponse(responseCode = "200", description = "Suppliers retrieved successfully")
    public ResponseEntity<Response> getSuppliersByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productSupplierService.getSuppliersByProduct(productId));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get products by supplier", description = "Retrieves all products for a specific supplier")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<Response> getProductsBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(productSupplierService.getProductsBySupplier(supplierId));
    }

    @GetMapping("/product/{productId}/supplier/{supplierId}")
    @Operation(summary = "Get product-supplier link", description = "Retrieves a specific product-supplier relationship with tracking data")
    @ApiResponse(responseCode = "200", description = "Link retrieved successfully")
    public ResponseEntity<Response> getProductSupplierLink(
            @PathVariable Long productId,
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(productSupplierService.getProductSupplierLink(productId, supplierId));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Add product-supplier link", description = "Creates a new product-supplier relationship (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Link created successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> addLink(@RequestBody ProductSupplier productSupplier) {
        return ResponseEntity.ok(productSupplierService.addProductSupplier(productSupplier));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update product-supplier link", description = "Updates an existing product-supplier relationship (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Link updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateLink(@RequestBody ProductSupplier productSupplier) {
        return ResponseEntity.ok(productSupplierService.updateProductSupplier(productSupplier));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove product-supplier link", description = "Removes a product-supplier relationship (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Link removed successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> removeLink(
            @RequestParam Long productId,
            @RequestParam Long supplierId) {
        return ResponseEntity.ok(productSupplierService.removeProductSupplier(productId, supplierId));
    }
}
