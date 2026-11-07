package org.pdm.backend.repository;

import org.pdm.backend.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository {
    /**
     * Saves a new product.
     *
     * @param productToSave The product to save.
     * @return The saved Product object.
     */
    Product save(Product productToSave);

    /**
     * Updates an existing product.
     *
     * @param productToUpdate The product to update.
     * @return The updated Product object.
     */
    Product update(Product productToUpdate);

    /**
     * Retrieves all products matching the filters.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param minPrice Filter by minimum price.
     * @param maxPrice Filter by maximum price.
     * @param stockStatus Filter by stock status.
     * @param search Search query for name or description.
     * @return A list of matching products.
     */
    List<Product> findAll(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search);

    /**
     * Retrieves a paginated and filtered list of products.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param minPrice Filter by minimum price.
     * @param maxPrice Filter by maximum price.
     * @param stockStatus Filter by stock status.
     * @param search Search query for name or description.
     * @param page The page number.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A list of filtered and paginated products.
     */
    List<Product> findAllPaged(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search, int page, int size, String sortBy, String sortDir);

    /**
     * Counts the total number of products matching the filters.
     *
     * @param categoryId Filter by category ID.
     * @param warehouseId Filter by warehouse ID.
     * @param minPrice Filter by minimum price.
     * @param maxPrice Filter by maximum price.
     * @param stockStatus Filter by stock status.
     * @param search Search query for name or description.
     * @return The total count of matching products.
     */
    int countAll(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search);

    /**
     * Retrieves a product by its ID.
     *
     * @param id The ID of the product.
     * @return An Optional containing the product if found.
     */
    Optional<Product> findById(Long id);

    /**
     * Finds products where name or description contains the search term.
     *
     * @param name Search term for name.
     * @param description Search term for description.
     * @return A list of matching products.
     */
    List<Product> findByNameContainingOrDescriptionContaining(String name, String description);

    /**
     * Deletes a product by its ID (soft delete).
     *
     * @param id The ID of the product to delete.
     * @return The number of affected rows.
     */
    Long deleteById(Long id);
}
