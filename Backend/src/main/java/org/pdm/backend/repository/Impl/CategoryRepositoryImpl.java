package org.pdm.backend.repository.Impl;

import org.pdm.backend.model.Category;
import org.pdm.backend.repository.CategoryRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.util.JdbcUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "name", "description", "display_order", "created_at"
    );

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    private final RowMapper<Category> mapRowToCategories = (rs, rowNum) -> {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setImageUrl(rs.getString("image_url"));
        category.setIsActive(rs.getBoolean("is_active"));
        category.setDisplayOrder(rs.getInt("display_order"));
        if (rs.getTimestamp("created_at") != null) {
            category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            category.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return category;
    };

    /**
     * Saves or updates a category in the database.
     *
     * @param category The category to save or update.
     * @return The saved or updated Category object.
     */
    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            String sql = "INSERT INTO categories (name, description, image_url, is_active, display_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, category.getName());
                ps.setString(2, category.getDescription());
                ps.setString(3, category.getImageUrl());
                ps.setBoolean(4, category.getIsActive() != null ? category.getIsActive() : true);
                ps.setInt(5, category.getDisplayOrder() != null ? category.getDisplayOrder() : 0);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                category.setId(key.longValue());
            }
            return category;
        } else {
            String sql = "UPDATE categories SET name = ?, description = ?, image_url = ?, is_active = ?, display_order = ?, updated_at = NOW() WHERE id = ?";
            jdbcTemplate.update(sql,
                    category.getName(),
                    category.getDescription(),
                    category.getImageUrl(),
                    category.getIsActive() != null ? category.getIsActive() : true,
                    category.getDisplayOrder() != null ? category.getDisplayOrder() : 0,
                    category.getId());
            return category;
        }
    }

    /**
     * Retrieves a category by its ID.
     *
     * @param id The ID of the category.
     * @return An Optional containing the category if found.
     */
    @Override
    public Optional<Category> findById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        List<Category> results = jdbcTemplate.query(sql, mapRowToCategories, id);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    /**
     * Retrieves all categories ordered by display order.
     *
     * @return A list of all categories.
     */
    @Override
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, mapRowToCategories);
    }

    /**
     * Deletes a category by its ID.
     *
     * @param id The ID of the category to delete.
     */
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Retrieves a category by its name.
     *
     * @param name The name of the category.
     * @return The category if found, otherwise null.
     */
    @Override
    public Category findByName(String name) {
        String sql = "SELECT * FROM categories WHERE name = ?";
        List<Category> results = jdbcTemplate.query(sql, mapRowToCategories, name);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Retrieves a paginated and filtered list of categories.
     *
     * @param filter Search query for name.
     * @param description Filter by description.
     * @param status Filter by status.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of filtered and paginated categories.
     */
    @Override
    public List<Category> findAllFilteredPaged(String filter, String description, String status, String sortBy, String sortDir, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM categories");
        List<Object> params = new ArrayList<>();
        appendCategoryFilters(sql, params, filter, description, status);
        sql.append(JdbcUtils.buildOrderClause(sortBy, sortDir, ALLOWED_SORT_COLUMNS));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);
        return jdbcTemplate.query(sql.toString(), mapRowToCategories, params.toArray());
    }

    /**
     * Counts the total number of categories matching the filters.
     *
     * @param filter Search query for name.
     * @param description Filter by description.
     * @param status Filter by status.
     * @return The total count of matching categories.
     */
    @Override
    public long countFiltered(String filter, String description, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM categories");
        List<Object> params = new ArrayList<>();
        appendCategoryFilters(sql, params, filter, description, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private void appendCategoryFilters(StringBuilder sql, List<Object> params,
                                        String filter, String description, String status) {
        boolean hasFilter = (filter != null && !filter.isBlank());
        boolean hasDescription = (description != null && !description.isBlank());
        boolean hasStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"));
        if (!hasFilter && !hasDescription && !hasStatus) return;
        sql.append(" WHERE 1=1");
        if (hasFilter) {
            sql.append(" AND name LIKE ?");
            params.add("%" + filter + "%");
        }
        if (hasDescription) {
            sql.append(" AND description LIKE ?");
            params.add("%" + description + "%");
        }
        if (hasStatus) {
            sql.append(" AND is_active = ?");
            params.add(status.equalsIgnoreCase("ACTIVE"));
        }
    }

}