package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.Supplier;
import org.pdm.backend.service.SupplierService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier management endpoints")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a new supplier", description = "Creates a new supplier (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Supplier created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> addSupplier(@RequestBody @Valid Supplier supplier) {
        return ResponseEntity.ok(supplierService.createSupplier(supplier));
    }


    @GetMapping("/all")
    @Operation(summary = "Get all suppliers", description = "Retrieves paginated list of suppliers with optional filters")
    @ApiResponse(responseCode = "200", description = "Suppliers retrieved successfully")
    public ResponseEntity<Response> getAllSuppliers(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taxCode,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(supplierService.getAllSuppliers(input, status, taxCode, sortBy, sortDir, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID", description = "Retrieves a single supplier by its ID")
    @ApiResponse(responseCode = "200", description = "Supplier found")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    public ResponseEntity<Response> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a supplier", description = "Updates an existing supplier (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Supplier updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateSupplier(@PathVariable Long id, @RequestBody @Valid Supplier supplier) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplier));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a supplier", description = "Soft deletes a supplier (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Supplier deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> deleteSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.deleteSupplier(id));
    }

    @PutMapping("/toggle-status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Toggle supplier status", description = "Toggles supplier between ACTIVE and INACTIVE (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Supplier status toggled successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> toggleSupplierStatus(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.toggleSupplierStatus(id));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Export suppliers to CSV", description = "Exports suppliers as a CSV file (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<String> exportSuppliersCsv(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String status) {
        String csv = supplierService.exportSuppliersCsv(input, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=suppliers.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}