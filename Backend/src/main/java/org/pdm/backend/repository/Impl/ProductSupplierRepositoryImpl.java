package org.pdm.backend.repository.Impl;

import org.pdm.backend.model.ProductSupplier;
import org.pdm.backend.repository.ProductSupplierRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductSupplierRepositoryImpl implements ProductSupplierRepository {

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    private final RowMapper<ProductSupplier> mapRowToProductSupplier = (rs, rowNum) -> {
        ProductSupplier ps = new ProductSupplier();
        ps.setProductId(rs.getLong("product_id"));
        ps.setSupplierId(rs.getLong("supplier_id"));
        ps.setUnitCost(rs.getBigDecimal("unit_cost"));
        ps.setIsPreferred(rs.getBoolean("is_preferred"));
        if (rs.getTimestamp("created_at") != null) {
            ps.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        // tracking columns
        ps.setTotalPurchased(rs.getLong("total_purchased"));
        ps.setTotalReturned(rs.getLong("total_returned"));
        // joined fields — null-safe (only present when query includes suppliers join)
        String supplierName = rs.getString("supplier_name");
        if (supplierName != null) {
            ps.setSupplierName(supplierName);
        }
        try {
            String contact = rs.getString("contact_info");
            ps.setSupplierContact(contact);
        } catch (Exception ignored) {
            // contact_info not present in result set
        }
        return ps;
    };

    @Override
    public List<ProductSupplier> findByProductId(Long productId) {
        String sql = """
                SELECT
                    ps.product_id,
                    ps.supplier_id,
                    ps.unit_cost,
                    ps.is_preferred,
                    ps.created_at,
                    COALESCE(tx.total_purchased, 0) AS total_purchased,
                    COALESCE(tx.total_returned, 0) AS total_returned,
                    s.name AS supplier_name,
                    s.contact_info
                FROM product_suppliers ps
                JOIN suppliers s ON s.id = ps.supplier_id
                LEFT JOIN (
                    SELECT
                        product_id,
                        supplier_id,
                        SUM(CASE WHEN transaction_type = 'PURCHASE' THEN total_products ELSE 0 END) AS total_purchased,
                        SUM(CASE WHEN transaction_type = 'RETURN_TO_SUPPLIER' THEN total_products ELSE 0 END) AS total_returned
                    FROM transactions
                    WHERE status = 'COMPLETED'
                    GROUP BY product_id, supplier_id
                ) tx ON tx.product_id = ps.product_id AND tx.supplier_id = ps.supplier_id
                WHERE ps.product_id = ?
                ORDER BY ps.is_preferred DESC
                """;
        return jdbcTemplate.query(sql, mapRowToProductSupplier, productId);
    }

    @Override
    public List<ProductSupplier> findBySupplierId(Long supplierId) {
        String sql = """
                SELECT
                    ps.product_id,
                    ps.supplier_id,
                    ps.unit_cost,
                    ps.is_preferred,
                    ps.created_at,
                    COALESCE(tx.total_purchased, 0) AS total_purchased,
                    COALESCE(tx.total_returned, 0) AS total_returned,
                    s.name AS supplier_name,
                    s.contact_info
                FROM product_suppliers ps
                JOIN suppliers s ON s.id = ps.supplier_id
                LEFT JOIN (
                    SELECT
                        product_id,
                        supplier_id,
                        SUM(CASE WHEN transaction_type = 'PURCHASE' THEN total_products ELSE 0 END) AS total_purchased,
                        SUM(CASE WHEN transaction_type = 'RETURN_TO_SUPPLIER' THEN total_products ELSE 0 END) AS total_returned
                    FROM transactions
                    WHERE status = 'COMPLETED'
                    GROUP BY product_id, supplier_id
                ) tx ON tx.product_id = ps.product_id AND tx.supplier_id = ps.supplier_id
                WHERE ps.supplier_id = ?
                ORDER BY ps.product_id ASC
                """;
        return jdbcTemplate.query(sql, mapRowToProductSupplier, supplierId);
    }

    @Override
    public List<ProductSupplier> findAll() {
        String sql = """
                SELECT
                    ps.product_id,
                    ps.supplier_id,
                    ps.unit_cost,
                    ps.is_preferred,
                    ps.created_at,
                    COALESCE(tx.total_purchased, 0) AS total_purchased,
                    COALESCE(tx.total_returned, 0) AS total_returned,
                    s.name AS supplier_name,
                    s.contact_info
                FROM product_suppliers ps
                JOIN suppliers s ON s.id = ps.supplier_id
                LEFT JOIN (
                    SELECT
                        product_id,
                        supplier_id,
                        SUM(CASE WHEN transaction_type = 'PURCHASE' THEN total_products ELSE 0 END) AS total_purchased,
                        SUM(CASE WHEN transaction_type = 'RETURN_TO_SUPPLIER' THEN total_products ELSE 0 END) AS total_returned
                    FROM transactions
                    WHERE status = 'COMPLETED'
                    GROUP BY product_id, supplier_id
                ) tx ON tx.product_id = ps.product_id AND tx.supplier_id = ps.supplier_id
                ORDER BY ps.product_id ASC, ps.is_preferred DESC
                """;
        return jdbcTemplate.query(sql, mapRowToProductSupplier);
    }

    @Override
    public Optional<ProductSupplier> findByProductIdAndSupplierId(Long productId, Long supplierId) {
        String sql = """
                SELECT
                    ps.product_id,
                    ps.supplier_id,
                    ps.unit_cost,
                    ps.is_preferred,
                    ps.created_at,
                    COALESCE(tx.total_purchased, 0) AS total_purchased,
                    COALESCE(tx.total_returned, 0) AS total_returned,
                    s.name AS supplier_name,
                    s.contact_info
                FROM product_suppliers ps
                JOIN suppliers s ON s.id = ps.supplier_id
                LEFT JOIN (
                    SELECT
                        product_id,
                        supplier_id,
                        SUM(CASE WHEN transaction_type = 'PURCHASE' THEN total_products ELSE 0 END) AS total_purchased,
                        SUM(CASE WHEN transaction_type = 'RETURN_TO_SUPPLIER' THEN total_products ELSE 0 END) AS total_returned
                    FROM transactions
                    WHERE status = 'COMPLETED'
                    GROUP BY product_id, supplier_id
                ) tx ON tx.product_id = ps.product_id AND tx.supplier_id = ps.supplier_id
                WHERE ps.product_id = ? AND ps.supplier_id = ?
                """;
        List<ProductSupplier> results = jdbcTemplate.query(sql, mapRowToProductSupplier, productId, supplierId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public ProductSupplier save(ProductSupplier productSupplier) {
        String sql = "INSERT INTO product_suppliers (product_id, supplier_id, unit_cost, is_preferred, created_at) VALUES (?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql,
                productSupplier.getProductId(),
                productSupplier.getSupplierId(),
                productSupplier.getUnitCost(),
                productSupplier.getIsPreferred() != null ? productSupplier.getIsPreferred() : false);
        return productSupplier;
    }

    @Override
    public ProductSupplier update(ProductSupplier productSupplier) {
        String sql = "UPDATE product_suppliers SET unit_cost = ?, is_preferred = ? WHERE product_id = ? AND supplier_id = ?";
        jdbcTemplate.update(sql,
                productSupplier.getUnitCost(),
                productSupplier.getIsPreferred() != null ? productSupplier.getIsPreferred() : false,
                productSupplier.getProductId(),
                productSupplier.getSupplierId());
        return productSupplier;
    }

    @Override
    public void deleteByProductIdAndSupplierId(Long productId, Long supplierId) {
        String sql = "DELETE FROM product_suppliers WHERE product_id = ? AND supplier_id = ?";
        jdbcTemplate.update(sql, productId, supplierId);
    }

    @Override
    public void clearPreferredForProduct(Long productId) {
        String sql = "UPDATE product_suppliers SET is_preferred = FALSE WHERE product_id = ?";
        jdbcTemplate.update(sql, productId);
    }

    @Override
    public void clearPreferredForProductExcept(Long productId, Long supplierId) {
        String sql = "UPDATE product_suppliers SET is_preferred = FALSE WHERE product_id = ? AND supplier_id <> ?";
        jdbcTemplate.update(sql, productId, supplierId);
    }

    @Override
    public void deleteByProductId(Long productId) {
        String sql = "DELETE FROM product_suppliers WHERE product_id = ?";
        jdbcTemplate.update(sql, productId);
    }

    @Override
    public void updatePurchaseReturnQuantities(Long productId, Long supplierId, Long purchasedDelta, Long returnedDelta) {
        // Return availability is derived from completed transactions for compatibility
        // with existing databases that do not have tracking columns.
    }
}
