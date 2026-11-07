package org.pdm.backend.repository.Impl;

import org.pdm.backend.enums.UserRole;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.pdm.backend.util.JdbcUtils;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "name", "email", "phone_number", "role", "is_active", "created_at", "updated_at"
    );

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId The unique identifier of the user.
     * @return An Optional containing the user if found.
     */
    @Override
    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try(Connection conn= DatabaseConfig.getConnection();){
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return Optional.of(mapRowToUsers(rs));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    /**
     * Retrieves a user by their email address.
     *
     * @param email The email of the user to search for.
     * @return An Optional containing the user if found.
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try(Connection conn= DatabaseConfig.getConnection();){
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return Optional.of(mapRowToUsers(rs));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    /**
     * Retrieves all users with dynamic sorting.
     *
     * @param sortBy  The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A list of all users.
     */
    public List<User> findAll(String sortBy, String sortDir) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users" + JdbcUtils.buildOrderClause(sortBy, sortDir, ALLOWED_SORT_COLUMNS);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUsers(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    /**
     * Retrieves a paginated list of users based on various filters.
     *
     * @param search    Keyword search for name, email, or phone number.
     * @param role      Filter by user role.
     * @param status    Filter by active status.
     * @param startDate Minimum creation date.
     * @param endDate   Maximum creation date.
     * @param sortBy    Field to sort by.
     * @param sortDir   Sorting direction (ASC/DESC).
     * @param page      Page number (0-indexed).
     * @param size      Page size.
     * @return A list of users for the requested page.
     * @throws RuntimeException if a database error occurs.
     */
    public List<User> findAllPaged(String search, String role, String status, String startDate, String endDate, String sortBy, String sortDir, int page, int size) {
        List<User> list = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users");

        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasRole = (role != null && !role.isBlank());
        boolean hasStatus = (status != null && !status.isBlank());
        boolean hasStart = (startDate != null && !startDate.isBlank());
        boolean hasEnd = (endDate != null && !endDate.isBlank());

        if (hasSearch || hasRole || hasStatus || hasStart || hasEnd) {
            sql.append(" WHERE 1=1");
            if (hasSearch) {
                sql.append(" AND (name LIKE ? OR email LIKE ? OR phone_number LIKE ?)");
                String pattern = "%" + search + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            if (hasRole) {
                sql.append(" AND role = ?");
                params.add(role);
            }
            if (hasStatus) {
                sql.append(" AND is_active = ?");
                params.add(status.equalsIgnoreCase("ACTIVE"));
            }
            if (hasStart) {
                sql.append(" AND created_at >= ?");
                params.add(Timestamp.valueOf(startDate + " 00:00:00"));
            }
            if (hasEnd) {
                sql.append(" AND created_at <= ?");
                params.add(Timestamp.valueOf(endDate + " 23:59:59"));
            }
        }

        String orderClause = JdbcUtils.buildOrderClause(sortBy, sortDir, ALLOWED_SORT_COLUMNS);
        sql.append(orderClause).append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUsers(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error querying paged users", e);
        }
        return list;
    }

    @Override
    /**
     * Counts the total number of users matching the specified filters.
     *
     * @param search    Keyword search.
     * @param role      Filter by user role.
     * @param status    Filter by active status.
     * @param startDate Minimum creation date.
     * @param endDate   Maximum creation date.
     * @return The total count of matching users.
     * @throws RuntimeException if a database error occurs.
     */
    public long countAll(String search, String role, String status, String startDate, String endDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users");

        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasRole = (role != null && !role.isBlank());
        boolean hasStatus = (status != null && !status.isBlank());
        boolean hasStart = (startDate != null && !startDate.isBlank());
        boolean hasEnd = (endDate != null && !endDate.isBlank());

        if (hasSearch || hasRole || hasStatus || hasStart || hasEnd) {
            sql.append(" WHERE 1=1");
            if (hasSearch) {
                sql.append(" AND (name LIKE ? OR email LIKE ? OR phone_number LIKE ?)");
                String pattern = "%" + search + "%";
                params.add(pattern);
                params.add(pattern);
                params.add(pattern);
            }
            if (hasRole) {
                sql.append(" AND role = ?");
                params.add(role);
            }
            if (hasStatus) {
                sql.append(" AND is_active = ?");
                params.add(status.equalsIgnoreCase("ACTIVE"));
            }
            if (hasStart) {
                sql.append(" AND created_at >= ?");
                params.add(Timestamp.valueOf(startDate + " 00:00:00"));
            }
            if (hasEnd) {
                sql.append(" AND created_at <= ?");
                params.add(Timestamp.valueOf(endDate + " 23:59:59"));
            }
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Database error counting users", e);
        }
        return 0;
    }

    @Override
    /**
     * Retrieves all active users who have a specific role.
     *
     * @param role The UserRole to filter by.
     * @return A list of users matching the role and active status.
     */
    public List<User> findByRole(UserRole role) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUsers(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    /**
     * Soft deletes a user by setting their active status to FALSE.
     *
     * @param id The ID of the user to deactivate.
     * @return 1L if successful, 0 otherwise.
     */
    public Long deleteById(Long id) {
        String sql = "UPDATE users SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setLong(1, id);
            ps.executeUpdate();
            return 1L;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    /**
     * Reactivates a user by setting their active status to TRUE.
     *
     * @param id The ID of the user to activate.
     * @return 1L if successful, 0 otherwise.
     */
    public Long activateById(Long id) {
        String sql = "UPDATE users SET is_active = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            return 1L;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    /**
     * Retrieves all users who are currently active.
     *
     * @return A list of all active users.
     */
    public List<User> findAllActive() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUsers(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    /**
     * Persists a new user in the database.
     *
     * @param userToSave The user entity to save.
     * @return The saved user with its generated ID.
     * @throws RuntimeException if a database error occurs.
     */
    public User save(User userToSave) {
        String sql = "INSERT INTO users (name, email, password, phone_number, role, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, userToSave.getName());
            ps.setString(2, userToSave.getEmail());
            ps.setString(3, userToSave.getPassword());
            ps.setString(4, userToSave.getPhoneNumber());
            ps.setString(5, userToSave.getRole().toString());
            ps.setTimestamp(6, Timestamp.valueOf(userToSave.getCreatedAt()));
            ps.setBoolean(7, userToSave.getIsActive() == null || userToSave.getIsActive());
            int affectedRows= ps.executeUpdate();
            if(affectedRows == 0) throw new SQLException("Creating user failed, no rows affected.");
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                userToSave.setId(generatedKeys.getLong(1));
                return userToSave;
            }
            else throw new SQLException("Creating user failed, no ID obtained.");
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    /**
     * Updates an existing user's details.
     *
     * @param userToUpdate The user entity containing updated values.
     * @return The updated user entity.
     * @throws RuntimeException if a database error occurs.
     */
    public User update(User userToUpdate) {
        String sql = "UPDATE users SET name = ?, email = ?, password = ?, phone_number = ?, role = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userToUpdate.getName());
            ps.setString(2, userToUpdate.getEmail());
            ps.setString(3, userToUpdate.getPassword());
            ps.setString(4, userToUpdate.getPhoneNumber());
            ps.setString(5, userToUpdate.getRole().toString());
            ps.setLong(6, userToUpdate.getId());
            ps.executeUpdate();
            return userToUpdate;



        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    /**
     * Checks if a user already exists with the specified email address.
     *
     * @param email The email to check.
     * @return True if the email exists, false otherwise.
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking email existence: " + e.getMessage(), e);
        }
    }

    private User mapRowToUsers(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setCreatedAt(JdbcUtils.getLocalDateTime(rs, "created_at"));
        user.setUpdatedAt(JdbcUtils.getLocalDateTime(rs, "updated_at"));
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) user.setCreatedBy(createdBy);
        long updatedBy = rs.getLong("updated_by");
        if (!rs.wasNull()) user.setUpdatedBy(updatedBy);
        user.setIsActive(rs.getBoolean("is_active"));
        return user;
    }
}
