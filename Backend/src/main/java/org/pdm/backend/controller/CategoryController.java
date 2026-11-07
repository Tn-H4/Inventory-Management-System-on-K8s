package org.pdm.backend.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.Category;
import org.pdm.backend.service.CategoryService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a category", description = "Creates a new product category (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Category created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> createCategory(@RequestBody @Valid Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }


    @GetMapping("/all")
    @Operation(summary = "Get all categories", description = "Retrieves paginated list of categories with optional search")
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    public ResponseEntity<Response> getAllCategories(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "sortBy", required = false) String sortBy,
        @RequestParam(value = "sortDir", required = false) String sortDir) {
        return ResponseEntity.ok(categoryService.getAllCategories(page, limit, search, description, status, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves a single category by its ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public ResponseEntity<Response> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a category", description = "Updates an existing category (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Category updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateUser(@PathVariable Long id, @RequestBody @Valid Category category) {
        return ResponseEntity.ok(categoryService.updateCategory(id , category));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a category", description = "Soft deletes a category (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Category deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> deleteCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.deleteCategory(id));
    }

    @PutMapping("/toggle-status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Toggle category status", description = "Toggles category between ACTIVE and INACTIVE (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Category status toggled successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> toggleCategoryStatus(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.toggleCategoryStatus(id));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Export categories to CSV", description = "Exports categories as a CSV file (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<String> exportCategoriesCsv(
            @RequestParam(value = "search", required = false) String search) {
        String csv = categoryService.exportCategoriesCsv(search);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=categories.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

}