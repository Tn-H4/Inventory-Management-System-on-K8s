package org.pdm.backend.repository.Impl;

import org.pdm.backend.model.Warehouse;
import org.pdm.backend.repository.WarehouseRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.util.JdbcUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "name", "location", "capacity", "manager_name", "created_at"
    );

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    private final RowMapper<Warehouse> mapRowToWarehouse = (rs, rowNum) -> {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(rs.getLong("id"));
        warehouse.setName(rs.getString("name"));
        warehouse.setLocation(rs.getString("location"));
        warehouse.setAddress(rs.getString("address"));
        int capacity = rs.getInt("capacity");
        if (!rs.wasNull()) {
            warehouse.setCapacity(capacity);
        }
        warehouse.setManagerName(rs.getString("manager_name"));
        warehouse.setIsActive(rs.getBoolean("is_active"));
        warehouse.setCreatedAt(JdbcUtils.getLocalDateTime(rs, "created_at"));
        warehouse.setUpdatedAt(JdbcUtils.getLocalDateTime(rs, "updated_at"));
        return warehouse;
    };

    @Override
    /**
     * Retrieves all warehouses ordered by name ascending.
     *
     * @return A list of all warehouses.
     */
    public List<Warehouse> findAll() {
        String sql = "SELECT * FROM warehouses ORDER BY name ASC";
        return jdbcTemplate.query(sql, mapRowToWarehouse);
    }

    @Override
    /**
     * Retrieves a paginated list of warehouses based on search and capacity filters.
     *
     * @param search      Search keyword for name, location, address, or manager name.
     * @param status      Filter by active/inactive status.
     * @param minCapacity Minimum capacity filter.
     * @param maxCapacity Maximum capacity filter.
     * @param sortBy      Field to sort by.
     * @param sortDir     Sorting direction (ASC/DESC).
     * @param page        Page number (0-indexed).
     * @param size        Page size.
     * @return A list of warehouses for the requested page.
     */
    public List<Warehouse> findAllPaged(String search, String status, Integer minCapacity, Integer maxCapacity, String sortBy, String sortDir, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM warehouses");
        List<Object> params = new ArrayList<>();
        appendWarehouseFilters(sql, params, search, status, minCapacity, maxCapacity);
        sql.append(JdbcUtils.buildOrderClause(sortBy, sortDir, ALLOWED_SORT_COLUMNS));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);
        return jdbcTemplate.query(sql.toString(), mapRowToWarehouse, params.toArray());
    }

    @Override
    /**
     * Counts the total number of warehouses matching the specified filters.
     *
     * @param search      Search keyword.
     * @param status      Filter by active/inactive status.
     * @param minCapacity Minimum capacity filter.
     * @param maxCapacity Maximum capacity filter.
     * @return The total count of matching warehouses.
     */
    public long countAll(String search, String status, Integer minCapacity, Integer maxCapacity) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM warehouses");
        List<Object> params = new ArrayList<>();
        appendWarehouseFilters(sql, params, search, status, minCapacity, maxCapacity);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private void appendWarehouseFilters(StringBuilder sql, List<Object> params,
                                         String search, String status,
                                         Integer minCapacity, Integer maxCapacity) {
        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"));
        boolean hasMinCap = (minCapacity != null);
        boolean hasMaxCap = (maxCapacity != null);
        if (!hasSearch && !hasStatus && !hasMinCap && !hasMaxCap) return;
        sql.append(" WHERE 1=1");
        if (hasSearch) {
            sql.append(" AND (name LIKE ? OR location LIKE ? OR address LIKE ? OR manager_name LIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (hasStatus) {
            sql.append(" AND is_active = ?");
            params.add(status.equalsIgnoreCase("ACTIVE"));
        }
        if (hasMinCap) {
            sql.append(" AND capacity >= ?");
            params.add(minCapacity);
        }
        if (hasMaxCap) {
            sql.append(" AND capacity <= ?");
            params.add(maxCapacity);
        }
    }

    @Override
    /**
     * Finds a warehouse by its unique identifier.
     *
     * @param id The ID of the warehouse.
     * @return An Optional containing the warehouse if found.
     */
    public Optional<Warehouse> findById(Long id) {
        String sql = "SELECT * FROM warehouses WHERE id = ?";
        List<Warehouse> results = jdbcTemplate.query(sql, mapRowToWarehouse, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    /**
     * Persists a new warehouse in the database.
     *
     * @param warehouse The warehouse entity to save.
     * @return The saved warehouse with its generated ID.
     */
    public Warehouse save(Warehouse warehouse) {
        String sql = "INSERT INTO warehouses (name, location, address, capacity, manager_name, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, warehouse.getName());
            ps.setString(2, warehouse.getLocation());
            ps.setString(3, warehouse.getAddress());
            if (warehouse.getCapacity() != null) {
                ps.setInt(4, warehouse.getCapacity());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, warehouse.getManagerName());
            ps.setBoolean(6, warehouse.getIsActive() != null ? warehouse.getIsActive() : true);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            warehouse.setId(key.longValue());
        }
        return warehouse;
    }

    @Override
    /**
     * Updates an existing warehouse's information.
     *
     * @param warehouse The warehouse entity containing updated values.
     * @return The updated warehouse entity.
     */
    public Warehouse update(Warehouse warehouse) {
        String sql = "UPDATE warehouses SET name = ?, location = ?, address = ?, capacity = ?, manager_name = ?, is_active = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql,
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.getAddress(),
                warehouse.getCapacity(),
                warehouse.getManagerName(),
                warehouse.getIsActive() != null ? warehouse.getIsActive() : true,
                warehouse.getId());
        return warehouse;
    }

    @Override
    /**
     * Deletes a warehouse from the database by its ID.
     *
     * @param id The unique identifier of the warehouse.
     * @return The number of affected rows.
     */
    public Long deleteById(Long id) {
        String sql = "DELETE FROM warehouses WHERE id = ?";
        return (long) jdbcTemplate.update(sql, id);
    }
}
