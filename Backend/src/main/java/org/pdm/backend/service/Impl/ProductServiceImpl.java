package org.pdm.backend.service.Impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Category;
import org.pdm.backend.model.Product;
import org.pdm.backend.repository.CategoryRepository;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.ProductSupplierRepository;
import org.pdm.backend.repository.TransactionRepository;
import org.pdm.backend.service.ProductService;
import org.pdm.backend.service.StockNotificationService;
import org.pdm.backend.wrappers.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.pdm.backend.util.CsvExportUtils;

import java.math.BigDecimal;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RequiredArgsConstructor
@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final TransactionRepository transactionRepository;
    private final StockNotificationService stockNotificationService;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${app.upload.directory:uploads}")
    private String uploadDirectory;

    private Long getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.pdm.backend.security.AuthUser authUser) {
                return authUser.getUser().getId();
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID for audit: {}", e.getMessage());
        }
        return null;
    }


    /**
     * Saves a new product to the system and uploads its image.
     *
     * @param product The product details.
     * @param imageFile The image file to upload.
     * @return A Response indicating the result.
     * @throws NotFoundException if the category is not found.
     */
    @Override
    public Response saveProduct(Product product, MultipartFile imageFile) {

        // validate category
        Category category = categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        // create product object to save
        Product productToSave = new Product();
        productToSave.setName(product.getName());
        productToSave.setPrice(product.getPrice());
        productToSave.setStockQuantity(product.getStockQuantity());
        productToSave.setReorderThreshold(product.getReorderThreshold() != null ? product.getReorderThreshold() : 10);
        productToSave.setDescription(product.getDescription());
        productToSave.setExpiryDate(product.getExpiryDate());
        productToSave.setCategoryId(category.getId());
        productToSave.setWarehouseId(product.getWarehouseId());
        productToSave.setCreatedAt(java.time.LocalDateTime.now());
        productToSave.setCreatedBy(getCurrentUserId());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            productToSave.setImageUrl(imagePath);
        }

        Product savedProduct = productRepository.save(productToSave);
        stockNotificationService.checkAndNotify(savedProduct);

        return Response.builder()
                .status(200)
                .message("Product was successfully saved")
                .build();
    }

    /**
     * Updates an existing product's details and optionally its image.
     *
     * @param product The updated product details.
     * @param imageFile The new image file to upload (optional).
     * @return A Response indicating the result.
     * @throws NotFoundException if the product or category is not found.
     */
    @Override
    public Response updateProduct(Product product, MultipartFile imageFile) {

        Product existingProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // update image
        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            existingProduct.setImageUrl(imagePath);
        }

        // update category
        if (product.getCategoryId() != null && product.getCategoryId() > 0) {
            Category category = categoryRepository.findById(product.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            existingProduct.setCategoryId(category.getId());
        }

        // update fields
        if (product.getName() != null && !product.getName().isBlank()) {
            existingProduct.setName(product.getName());
        }
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            existingProduct.setDescription(product.getDescription());
        }
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getStockQuantity() != null && product.getStockQuantity() >= 0) {
            existingProduct.setStockQuantity(product.getStockQuantity());
        }
        if (product.getReorderThreshold() != null && product.getReorderThreshold() >= 0) {
            existingProduct.setReorderThreshold(product.getReorderThreshold());
        }
        if (product.getExpiryDate() != null) {
            existingProduct.setExpiryDate(product.getExpiryDate());
        }
        if (product.getWarehouseId() != null) {
            existingProduct.setWarehouseId(product.getWarehouseId());
        }

        existingProduct.setUpdatedBy(getCurrentUserId());
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

        productRepository.update(existingProduct);
        stockNotificationService.checkAndNotify(existingProduct);

        return Response.builder()
                .status(200)
                .message("Product was successfully updated")
                .build();
    }

    /**
     * Retrieves a paginated and filtered list of all products.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param minPrice Filter products with price >= minPrice.
     * @param maxPrice Filter products with price <= maxPrice.
     * @param stockStatus Filter by stock status (e.g., "LOW", "OUT").
     * @param search Search query for product name or SKU.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of products.
     */
    @Override
    public Response getAllProducts(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search, int page, int size, String sortBy, String sortDir) {

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        String safeSortBy = switch (sortBy == null ? "" : sortBy) {
            case "name", "price", "stock_quantity", "created_at", "id" -> sortBy;
            default -> "id";
        };
        String safeSortDir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        List<Product> products = productRepository.findAllPaged(categoryId, warehouseId, minPrice, maxPrice, stockStatus, search, safePage, safeSize, safeSortBy, safeSortDir);
        int total = productRepository.countAll(categoryId, warehouseId, minPrice, maxPrice, stockStatus, search);
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return Response.builder()
                .status(200)
                .message("success")
                .products(products)
                .totalElements((long) total)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Retrieves a specific product by its ID.
     *
     * @param id The ID of the product.
     * @return A Response containing the product details.
     * @throws NotFoundException if the product is not found.
     */
    @Override
    public Response getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product Not Found"));

        return Response.builder()
                .status(200)
                .message("success")
                .product(product)
                .build();
    }

    /**
     * Deletes a product by its ID (soft delete).
     *
     * @param id The ID of the product to delete.
     * @return A Response indicating the result.
     * @throws NotFoundException if the product is not found.
     */
    @Override
    public Response deleteProduct(Long id) {

        productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product Not Found"));

        // Cascade delete: remove related product_suppliers
        productSupplierRepository.deleteByProductId(id);

        // Cascade update: nullify product_id in transactions
        transactionRepository.nullifyProductId(id);

        // Soft delete the product
        productRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Product Deleted successfully")
                .build();
    }

    /**
     * Searches for products by name or SKU.
     *
     * @param input The search query.
     * @return A Response containing the list of matching products.
     */
    @Override
    public Response searchProducts(String input) {

        List<Product> products = productRepository.findAll(null, null, null, null, null, input);

        return Response.builder()
                .status(200)
                .message("success")
                .products(products)
                .totalElements((long) products.size())
                .totalPages(products.isEmpty() ? 0 : 1)
                .build();
    }

    /**
     * Exports filtered products to a CSV format.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param stockStatus Filter by stock status.
     * @param search Search query.
     * @return A CSV string containing the product data.
     */
    @Override
    public String exportProductsCsv(Long categoryId, Long warehouseId, String stockStatus, String search) {
        List<Product> products = productRepository.findAllPaged(categoryId, warehouseId, null, null, stockStatus, search, 1, 10_000, "id", "ASC");
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,SKU,Category ID,Warehouse ID,Price,Stock Quantity,Reorder Threshold,Description,Expiry Date,Created At\n");
        for (Product p : products) {
            csv.append(p.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(p.getName())).append(",");
            csv.append(CsvExportUtils.escapeCsv(p.getSku())).append(",");
            csv.append(p.getCategoryId() != null ? p.getCategoryId() : "").append(",");
            csv.append(p.getWarehouseId() != null ? p.getWarehouseId() : "").append(",");
            csv.append(p.getPrice() != null ? p.getPrice() : "").append(",");
            csv.append(p.getStockQuantity() != null ? p.getStockQuantity() : "").append(",");
            csv.append(p.getReorderThreshold() != null ? p.getReorderThreshold() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(p.getDescription())).append(",");
            csv.append(CsvExportUtils.isoDateTime(p.getExpiryDate())).append(",");
            csv.append(CsvExportUtils.isoDateTime(p.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    // Save image to folder
    private String saveImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String contentType = imageFile.getContentType();
        String extension = ALLOWED_IMAGE_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed");
        }

        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image must be under 5 MB");
        }

        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename != null) {
            String lowerFilename = Paths.get(originalFilename).getFileName().toString().toLowerCase(Locale.ROOT);
            boolean hasAllowedExtension = lowerFilename.endsWith(".jpg")
                    || lowerFilename.endsWith(".jpeg")
                    || lowerFilename.endsWith(".png")
                    || lowerFilename.endsWith(".webp");
            if (!hasAllowedExtension) {
                throw new IllegalArgumentException("Image filename must end with .jpg, .jpeg, .png, or .webp");
            }
        }

        Path productImageDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize().resolve("products");
        String uniqueFileName = UUID.randomUUID() + extension;
        Path destinationPath = productImageDirectory.resolve(uniqueFileName).normalize();

        try {
            Files.createDirectories(productImageDirectory);
            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error saving Image: " + e.getMessage());
        }

        return "/uploads/products/" + uniqueFileName;
    }
}
