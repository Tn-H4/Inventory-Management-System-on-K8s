package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.Warehouse;
import org.pdm.backend.service.WarehouseService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse management endpoints")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/all")
    @Operation(summary = "Get all warehouses", description = "Retrieves paginated list of warehouses with optional filters")
    @ApiResponse(responseCode = "200", description = "Warehouses retrieved successfully")
    public ResponseEntity<Response> getAllWarehouses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(warehouseService.getAllWarehouses(search, status, minCapacity, maxCapacity, sortBy, sortDir, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID", description = "Retrieves a single warehouse by its ID")
    @ApiResponse(responseCode = "200", description = "Warehouse found")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    public ResponseEntity<Response> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a warehouse", description = "Creates a new warehouse (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Warehouse created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> createWarehouse(@RequestBody @Valid Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.createWarehouse(warehouse));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a warehouse", description = "Updates an existing warehouse (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Warehouse updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateWarehouse(@PathVariable Long id, @RequestBody @Valid Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, warehouse));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a warehouse", description = "Soft deletes a warehouse (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Warehouse deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> deleteWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.deleteWarehouse(id));
    }

    @PutMapping("/toggle-status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Toggle warehouse status", description = "Toggles warehouse between ACTIVE and INACTIVE (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Warehouse status toggled successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> toggleWarehouseStatus(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.toggleWarehouseStatus(id));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Export warehouses to CSV", description = "Exports warehouses as a CSV file (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<String> exportWarehousesCsv() {
        String csv = warehouseService.exportWarehousesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=warehouses.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
