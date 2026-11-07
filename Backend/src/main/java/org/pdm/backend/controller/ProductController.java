package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.pdm.backend.model.Product;
import org.springframework.validation.annotation.Validated;
import org.pdm.backend.service.ProductService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a new product", description = "Creates a new product with image upload (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation error")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> saveProduct(
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam(value = "reorderThreshold", required = false) Integer reorderThreshold,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "description", required = false) String description
    ) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setReorderThreshold(reorderThreshold);
        product.setCategoryId(categoryId);
        product.setWarehouseId(warehouseId);
        product.setDescription(description);

        return ResponseEntity.ok(productService.saveProduct(product, imageFile));

    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a product", description = "Updates an existing product with optional image upload (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Product updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation error")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateProduct(
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
            @RequestParam(value = "reorderThreshold", required = false) Integer reorderThreshold,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("productId") Long productId
    ) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setId(productId);
        product.setStockQuantity(stockQuantity);
        product.setReorderThreshold(reorderThreshold);
        product.setCategoryId(categoryId);
        product.setWarehouseId(warehouseId);
        product.setDescription(description);

        return ResponseEntity.ok(productService.updateProduct(product, imageFile));

    }


    @GetMapping("/all")
    @Operation(summary = "Get all products", description = "Retrieves paginated list of products with optional filters")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<Response> getAllProducts(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "stockStatus", required = false) String stockStatus,
            @RequestParam(value = "search", required = false) @Size(max = 100) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "ASC") String sortDir) {
        return ResponseEntity.ok(productService.getAllProducts(categoryId, warehouseId, minPrice, maxPrice, stockStatus, search, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a single product by its ID")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<Response> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a product", description = "Soft deletes a product (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Product deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name or SKU")
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    public ResponseEntity<Response> searchProduct(@RequestParam @Size(min = 1, max = 100) String input) {
        return ResponseEntity.ok(productService.searchProducts(input));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Export products to CSV", description = "Exports products as a CSV file (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN or MANAGER required")
    public ResponseEntity<String> exportProductsCsv(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "stockStatus", required = false) String stockStatus,
            @RequestParam(value = "search", required = false) String search) {
        String csv = productService.exportProductsCsv(categoryId, warehouseId, stockStatus, search);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

}

