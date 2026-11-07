package org.pdm.backend.service;

import org.pdm.backend.model.Product;
import org.pdm.backend.wrappers.Response;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface ProductService {
    /**
     * Saves a new product and its associated image.
     *
     * @param product The product details to save.
     * @param imagefile The image file to upload.
     * @return A Response indicating the result of the product creation.
     */
    Response saveProduct(Product product, MultipartFile imagefile);

    /**
     * Updates an existing product and optionally its associated image.
     *
     * @param product The updated product details.
     * @param imagefile The new image file to upload, if any.
     * @return A Response indicating the result of the product update.
     * @throws org.pdm.backend.exception.NotFoundException if the product is not found.
     */
    Response updateProduct(Product product, MultipartFile imagefile);

    /**
     * Retrieves a paginated and filtered list of all products.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param minPrice Minimum price filter.
     * @param maxPrice Maximum price filter.
     * @param stockStatus Filter by stock status (e.g., IN_STOCK, LOW_STOCK, OUT_OF_STOCK).
     * @param search Search query for product name, SKU, or description.
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of products.
     */
    Response getAllProducts(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search, int page, int size, String sortBy, String sortDir);

    /**
     * Retrieves a specific product by its ID.
     *
     * @param id The ID of the product.
     * @return A Response containing the product details.
     * @throws org.pdm.backend.exception.NotFoundException if the product is not found.
     */
    Response getProductById(Long id);

    /**
     * Searches for products based on a search input.
     *
     * @param input The search query.
     * @return A Response containing the list of matching products.
     */
    Response searchProducts(String input);

    /**
     * Deletes a product by its ID (soft delete).
     *
     * @param id The ID of the product to delete.
     * @return A Response indicating the result of the deletion.
     * @throws org.pdm.backend.exception.NotFoundException if the product is not found.
     */
    Response deleteProduct(Long id);

    /**
     * Exports a filtered list of products to a CSV format.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param stockStatus Filter by stock status.
     * @param search Search query.
     * @return A CSV string containing the product data.
     */
    String exportProductsCsv(Long categoryId, Long warehouseId, String stockStatus, String search);
}
