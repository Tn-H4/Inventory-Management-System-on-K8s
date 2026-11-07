package org.pdm.backend.repository.Impl;

import org.pdm.backend.enums.SupplierStatus;
import org.pdm.backend.model.Supplier;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class SupplierRepositoryImpl implements SupplierRepository {

    private static final List<String> ALLOWED_SORT_COLUMNS = List.of(
        "name", "email", "tax_code", "created_at"
    );

    @Override
    /**
     * Retrieves a supplier by its unique identifier.
     *
     * @param id The ID of the supplier.
     * @return An Optional containing the supplier if found.
     */
    public Optional<Supplier> findById(Long id) {
        String sql = "SELECT * FROM suppliers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToSupplier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    /**
     * Retrieves all suppliers ordered by name ascending.
     *
     * @return A list of all suppliers.
     */
    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToSupplier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    /**
     * Retrieves a paginated list of suppliers based on search filters and sorting criteria.
     *
     * @param input    Search keyword for name, contact info, address, or email.
     * @param status   Filter by supplier status.
     * @param taxCode  Filter by tax code.
     * @param sortBy   Field to sort by.
     * @param sortDir  Sorting direction (ASC/DESC).
     * @param page     Page number (1-indexed).
     * @param size     Page size.
     * @return A list of suppliers for the requested page.
     * @throws RuntimeException if a database error occurs.
     */
    public List<Supplier> findAllPaged(String input, String status, String taxCode, String sortBy, String sortDir, int page, int size) {
        List<Supplier> list = new ArrayList<>();
        List<String> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM suppliers");
        appendWhereClause(sql, params, input, status, taxCode);

        String orderClause = " ORDER BY name ASC";
        if (sortBy != null && !sortBy.isBlank()) {
            // Map known camelCase fields to snake_case; unknown fields fall through to whitelist check.
            String column = switch (sortBy) {
                case "taxCode" -> "tax_code";
                case "createdAt" -> "created_at";
                default -> sortBy.toLowerCase();
            };
            boolean isValidColumn = ALLOWED_SORT_COLUMNS.contains(column);
            if (isValidColumn) {
                String direction = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
                orderClause = " ORDER BY " + column + " " + direction;
            }
        }
        sql.append(orderClause).append(" LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String p : params) ps.setString(idx++, p);
            ps.setInt(idx++, size);
            ps.setInt(idx++, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToSupplier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error querying paged suppliers: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    /**
     * Counts the total number of suppliers matching the given search and status filters.
     *
     * @param input    Search keyword.
     * @param status   Filter by supplier status.
     * @param taxCode  Filter by tax code.
     * @return The total count of matching suppliers.
     * @throws RuntimeException if a database error occurs.
     */
    public int countAll(String input, String status, String taxCode) {
        List<String> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM suppliers");
        appendWhereClause(sql, params, input, status, taxCode);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String p : params) ps.setString(idx++, p);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting suppliers: " + e.getMessage(), e);
        }
        return 0;
    }

    private void appendWhereClause(StringBuilder sql, List<String> params, String input, String status, String taxCode) {
        boolean hasSearch = input != null && !input.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasTaxCode = taxCode != null && !taxCode.isBlank();
        if (!hasSearch && !hasStatus && !hasTaxCode) return;
        sql.append(" WHERE");
        if (hasSearch) {
            sql.append(" (name LIKE ? OR contact_info LIKE ? OR address LIKE ? OR email LIKE ?)");
            String filter = "%" + input + "%";
            params.add(filter);
            params.add(filter);
            params.add(filter);
            params.add(filter);
        }
        if (hasSearch && (hasStatus || hasTaxCode)) sql.append(" AND");
        if (hasStatus) {
            sql.append(" status = ?");
            params.add(status);
        }
        if (hasStatus && hasTaxCode) sql.append(" AND");
        if (hasTaxCode) {
            sql.append(" tax_code = ?");
            params.add(taxCode);
        }
    }

    @Override
    /**
     * Persists a new supplier in the database.
     *
     * @param supplierToSave The supplier entity to save.
     * @return The saved supplier with its generated ID.
     * @throws RuntimeException if a database error occurs.
     */
    public Supplier save(Supplier supplierToSave) {
        String sql = "INSERT INTO suppliers (name, address, contact_info, email, website, tax_code, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, supplierToSave.getName());
            ps.setString(2, supplierToSave.getAddress());
            ps.setString(3, supplierToSave.getContactInfo());
            ps.setString(4, supplierToSave.getEmail());
            ps.setString(5, supplierToSave.getWebsite());
            ps.setString(6, supplierToSave.getTaxCode());
            ps.setString(7, supplierToSave.getStatus() != null ? supplierToSave.getStatus().name() : SupplierStatus.ACTIVE.name());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating supplier failed, no rows affected.");
            }
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                supplierToSave.setId(generatedKeys.getLong(1));
                return supplierToSave;
            } else {
                throw new SQLException("Creating supplier failed, no ID obtained.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    /**
     * Updates an existing supplier's information.
     *
     * @param supplierToUpdate The supplier entity containing updated values.
     * @return The updated supplier entity.
     * @throws RuntimeException if a database error occurs.
     */
    public Supplier update(Supplier supplierToUpdate) {
        String sql = "UPDATE suppliers SET name = ?, address = ?, contact_info = ?, email = ?, website = ?, tax_code = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplierToUpdate.getName());
            ps.setString(2, supplierToUpdate.getAddress());
            ps.setString(3, supplierToUpdate.getContactInfo());
            ps.setString(4, supplierToUpdate.getEmail());
            ps.setString(5, supplierToUpdate.getWebsite());
            ps.setString(6, supplierToUpdate.getTaxCode());
            ps.setString(7, supplierToUpdate.getStatus() != null ? supplierToUpdate.getStatus().name() : SupplierStatus.ACTIVE.name());
            ps.setLong(8, supplierToUpdate.getId());
            ps.executeUpdate();
            return supplierToUpdate;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    /**
     * Deletes a supplier from the database by its ID.
     *
     * @param id The unique identifier of the supplier.
     * @return The number of affected rows (1 if successful, 0 otherwise).
     */
    public Long deleteById(Long id) {
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return (long) ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    /**
     * Searches for suppliers by any identifying field (name, contact, address, email).
     *
     * @param input The search keyword.
     * @return A list of matching suppliers.
     */
    public List<Supplier> searchSupplierByAnything(String input) {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE name LIKE ? OR contact_info LIKE ? OR address LIKE ? OR email LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String filter = "%" + input + "%";
            ps.setString(1, filter);
            ps.setString(2, filter);
            ps.setString(3, filter);
            ps.setString(4, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToSupplier(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private Supplier mapRowToSupplier(ResultSet rs) throws SQLException {
        Supplier supplier = Supplier.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .address(rs.getString("address"))
                .contactInfo(rs.getString("contact_info"))
                .email(rs.getString("email"))
                .website(rs.getString("website"))
                .taxCode(rs.getString("tax_code"))
                .build();
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            supplier.setStatus(SupplierStatus.valueOf(statusStr));
        }
        if (rs.getTimestamp("created_at") != null) {
            supplier.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            supplier.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return supplier;
    }
}
