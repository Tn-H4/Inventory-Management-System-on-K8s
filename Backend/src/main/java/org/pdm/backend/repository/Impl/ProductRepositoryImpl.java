package org.pdm.backend.repository.Impl;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.model.Product;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.util.JdbcUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    @Override
    /**
     * Saves a new product and generates a unique SKU based on its auto-incremented ID.
     * This operation is performed within a transaction.
     *
     * @param productToSave The product entity to persist.
     * @return The persisted product with its generated SKU.
     * @throws RuntimeException if a database error occurs.
     */
    public Product save(Product productToSave) {
        String sql = "INSERT INTO products (name, sku, price, stock_quantity, reorder_threshold, description, expiry_date, image_url, category_id, warehouse_id, created_at, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSkuSql = "UPDATE products SET sku = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setString(1, productToSave.getName());
            ps.setString(2, "PENDING-" + UUID.randomUUID());
            ps.setBigDecimal(3, productToSave.getPrice());
            ps.setInt(4, productToSave.getStockQuantity());
            ps.setInt(5, productToSave.getReorderThreshold() != null ? productToSave.getReorderThreshold() : 10);
            ps.setString(6, productToSave.getDescription());
            JdbcUtils.setTimestampOrNull(ps, 7, productToSave.getExpiryDate());
            ps.setString(8, productToSave.getImageUrl());
            ps.setLong(9, productToSave.getCategoryId());
            JdbcUtils.setLongOrNull(ps, 10, productToSave.getWarehouseId());
            JdbcUtils.setTimestampOrNull(ps, 11, productToSave.getCreatedAt());
            JdbcUtils.setLongOrNull(ps, 12, productToSave.getCreatedBy());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                productToSave.setId(rs.getLong(1));
            } else {
                throw new SQLException("No product ID returned after insert");
            }
            String generatedSku = String.format("SKU%06d", productToSave.getId());
            try (PreparedStatement updateSku = conn.prepareStatement(updateSkuSql)) {
                updateSku.setString(1, generatedSku);
                updateSku.setLong(2, productToSave.getId());
                updateSku.executeUpdate();
            }
            conn.commit();
            productToSave.setSku(generatedSku);
            return productToSave;
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving product: " + e.getMessage(), e);
        }
    }

    @Override
    /**
     * Updates an existing product's information.
     *
     * @param productToUpdate The product entity containing updated values.
     * @return The updated product entity.
     * @throws RuntimeException if a database error occurs.
     */
    public Product update(Product productToUpdate) {
        String sql = "UPDATE products SET name = ?, sku = ?, price = ?, stock_quantity = ?, reorder_threshold = ?, description = ?, expiry_date = ?, image_url = ?, category_id = ?, warehouse_id = ?, updated_at = NOW(), updated_by = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productToUpdate.getName());
            ps.setString(2, productToUpdate.getSku());
            ps.setBigDecimal(3, productToUpdate.getPrice());
            ps.setInt(4, productToUpdate.getStockQuantity());
            ps.setInt(5, productToUpdate.getReorderThreshold() != null ? productToUpdate.getReorderThreshold() : 10);
            ps.setString(6, productToUpdate.getDescription());
            JdbcUtils.setTimestampOrNull(ps, 7, productToUpdate.getExpiryDate());
            ps.setString(8, productToUpdate.getImageUrl());
            ps.setLong(9, productToUpdate.getCategoryId());
            JdbcUtils.setLongOrNull(ps, 10, productToUpdate.getWarehouseId());
            JdbcUtils.setLongOrNull(ps, 11, productToUpdate.getUpdatedBy());
            ps.setLong(12, productToUpdate.getId());
            ps.executeUpdate();
            return productToUpdate;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating product: " + e.getMessage(), e);
        }
    }

    private void buildFilterClause(StringBuilder sql, List<Object> params,
                                   Long categoryId, Long warehouseId,
                                   BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search) {
        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (warehouseId != null) {
            sql.append(" AND warehouse_id = ?");
            params.add(warehouseId);
        }
        if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }
        if (stockStatus != null && !stockStatus.isBlank()) {
            switch (stockStatus) {
                case "IN_STOCK":
                    sql.append(" AND stock_quantity > reorder_threshold");
                    break;
                case "LOW_STOCK":
                    sql.append(" AND stock_quantity > 0 AND stock_quantity <= reorder_threshold");
                    break;
                case "OUT_OF_STOCK":
                    sql.append(" AND stock_quantity = 0");
                    break;
            }
        }
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.trim() + "%";
            sql.append(" AND (name LIKE ? OR sku LIKE ? OR description LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (param instanceof Long) {
                ps.setLong(i + 1, (Long) param);
            } else if (param instanceof BigDecimal) {
                ps.setBigDecimal(i + 1, (BigDecimal) param);
            } else {
                ps.setString(i + 1, param.toString());
            }
        }
    }

    @Override
    /**
     * Retrieves all non-deleted products based on a set of filters.
     *
     * @param categoryId   Filter by category ID.
     * @param warehouseId  Filter by warehouse ID.
     * @param minPrice     Minimum price filter.
     * @param maxPrice     Maximum price filter.
     * @param stockStatus  Filter by stock status (IN_STOCK, LOW_STOCK, OUT_OF_STOCK).
     * @param search       Keyword search for name, SKU, or description.
     * @return A list of matching products.
     * @throws RuntimeException if a database error occurs.
     */
    public List<Product> findAll(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE is_deleted = FALSE");
        List<Object> params = new ArrayList<>();
        buildFilterClause(sql, params, categoryId, warehouseId, minPrice, maxPrice, stockStatus, search);
        sql.append(" ORDER BY id ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToProduct(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error querying products: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    /**
     * Retrieves a paginated list of non-deleted products based on filters and sorting.
     *
     * @param categoryId   Filter by category ID.
     * @param warehouseId  Filter by warehouse ID.
     * @param minPrice     Minimum price filter.
     * @param maxPrice     Maximum price filter.
     * @param stockStatus  Filter by stock status.
     * @param search       Keyword search.
     * @param page         Page number (1-indexed).
     * @param size         Page size.
     * @param sortBy       Field to sort by.
     * @param sortDir      Sorting direction (ASC/DESC).
     * @return A list of products for the requested page.
     * @throws RuntimeException if a database error occurs.
     */
    public List<Product> findAllPaged(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search, int page, int size, String sortBy, String sortDir) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE is_deleted = FALSE");
        List<Object> params = new ArrayList<>();
        buildFilterClause(sql, params, categoryId, warehouseId, minPrice, maxPrice, stockStatus, search);
        sql.append(JdbcUtils.buildOrderClause(sortBy, sortDir, java.util.Set.of("name", "price", "stock_quantity", "created_at", "id")));
        sql.append(" LIMIT ? OFFSET ?");
        params.add((long) size);
        params.add((long) ((page - 1) * size));

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToProduct(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error querying paged products: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    /**
     * Counts the total number of non-deleted products matching the filters.
     *
     * @param categoryId   Filter by category ID.
     * @param warehouseId  Filter by warehouse ID.
     * @param minPrice     Minimum price filter.
     * @param maxPrice     Maximum price filter.
     * @param stockStatus  Filter by stock status.
     * @param search       Keyword search.
     * @return The total count of matching products.
     * @throws RuntimeException if a database error occurs.
     */
    public int countAll(Long categoryId, Long warehouseId, BigDecimal minPrice, BigDecimal maxPrice, String stockStatus, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products WHERE is_deleted = FALSE");
        List<Object> params = new ArrayList<>();
        buildFilterClause(sql, params, categoryId, warehouseId, minPrice, maxPrice, stockStatus, search);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error counting products: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    /**
     * Finds a non-deleted product by its ID.
     *
     * @param id The unique identifier of the product.
     * @return An Optional containing the product if found.
     * @throws RuntimeException if a database error occurs.
     */
    public Optional<Product> findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ? AND is_deleted = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToProduct(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error finding product by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    /**
     * Searches for products by name or description.
     *
     * @param name        The name keyword.
     * @param description The description keyword.
     * @return A list of matching products.
     */
    public List<Product> findByNameContainingOrDescriptionContaining(String name, String description) {
        return findAll(null, null, null, null, null, name);
    }

    @Override
    /**
     * Performs a soft delete of a product by marking is_deleted = TRUE.
     *
     * @param id The ID of the product to delete.
     * @return 1L if successful.
     * @throws RuntimeException if a database error occurs.
     */
    public Long deleteById(Long id) {
        String sql = "UPDATE products SET is_deleted = TRUE, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            return 1L;
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting product: " + e.getMessage(), e);
        }
    }

    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setSku(rs.getString("sku"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setReorderThreshold(rs.getInt("reorder_threshold"));
        product.setDescription(rs.getString("description"));
        if (rs.getTimestamp("expiry_date") != null) {
            product.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
        }
        product.setImageUrl(rs.getString("image_url"));
        product.setCategoryId(rs.getLong("category_id"));
        long warehouseId = rs.getLong("warehouse_id");
        if (!rs.wasNull()) {
            product.setWarehouseId(warehouseId);
        }
        if (rs.getTimestamp("created_at") != null) {
            product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            product.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        product.setIsDeleted(rs.getBoolean("is_deleted"));
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
            product.setCreatedBy(createdBy);
        }
        long updatedBy = rs.getLong("updated_by");
        if (!rs.wasNull()) {
            product.setUpdatedBy(updatedBy);
        }
        return product;
    }
}
