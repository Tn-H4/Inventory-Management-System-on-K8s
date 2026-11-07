package org.pdm.backend.repository.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.model.AuditLog;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.AuditLogRepository;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import org.pdm.backend.util.JdbcUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS =
            Set.of("id", "action", "entity_type", "created_at");

    private final UserRepository userRepository;

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(DatabaseConfig.getDataSource());

    private final RowMapper<AuditLog> mapRowToAuditLog = (rs, rowNum) -> {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(rs.getLong("id"));
        auditLog.setUserId(rs.getLong("user_id"));
        auditLog.setAction(rs.getString("action"));
        auditLog.setEntityType(rs.getString("entity_type"));
        auditLog.setEntityId(rs.getLong("entity_id"));
        auditLog.setDetails(rs.getString("details"));
        if (rs.getTimestamp("created_at") != null) {
            auditLog.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return auditLog;
    };

    private AuditLog hydrateAuditLog(AuditLog auditLog) {
        if (auditLog.getUserId() != null) {
            auditLog.setUser(
                    userRepository.findById(auditLog.getUserId())
                            .map(this::sanitizeUser)
                            .orElse(null)
            );
        }
        return auditLog;
    }

    private User sanitizeUser(User user) {
        user.setPassword(null);
        return user;
    }

    /**
     * Saves a new audit log entry.
     *
     * @param auditLog The audit log to save.
     * @return The saved AuditLog object, hydrated with user information.
     */
    @Override
    public AuditLog save(AuditLog auditLog) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, auditLog.getUserId());
            ps.setString(2, auditLog.getAction());
            ps.setString(3, auditLog.getEntityType());
            ps.setLong(4, auditLog.getEntityId());
            ps.setString(5, auditLog.getDetails());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            auditLog.setId(key.longValue());
        }
        return hydrateAuditLog(auditLog);
    }

    /**
     * Retrieves an audit log entry by its ID.
     *
     * @param id The ID of the audit log.
     * @return An Optional containing the hydrated audit log if found.
     */
    @Override
    public Optional<AuditLog> findById(Long id) {
        String sql = "SELECT * FROM audit_logs WHERE id = ?";
        List<AuditLog> results = jdbcTemplate.query(sql, mapRowToAuditLog, id);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hydrateAuditLog(results.get(0)));
    }

    /**
     * Retrieves a filtered, paginated list of audit logs.
     */
    @Override
    public List<AuditLog> findAllPaged(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate,
            String sortBy, String sortDir,
            int page, int size) {

        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, search, action, entityType, startDate, endDate);

        sql.append(JdbcUtils.buildOrderClause(sortBy, sortDir, ALLOWED_SORT_COLUMNS));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        return jdbcTemplate.query(sql.toString(), mapRowToAuditLog, params.toArray())
                .stream()
                .map(this::hydrateAuditLog)
                .toList();
    }

    /**
     * Counts audit logs matching the given filters.
     */
    @Override
    public long countFiltered(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate) {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_logs");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, search, action, entityType, startDate, endDate);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search, String action, String entityType,
                               LocalDate startDate, LocalDate endDate) {
        boolean hasSearch     = search    != null && !search.isBlank();
        boolean hasAction     = action    != null && !action.isBlank();
        boolean hasEntityType = entityType != null && !entityType.isBlank();

        if (hasSearch || hasAction || hasEntityType || startDate != null || endDate != null) {
            sql.append(" WHERE 1=1");
            if (hasSearch) {
                sql.append(" AND details LIKE ?");
                params.add("%" + search + "%");
            }
            if (hasAction) {
                sql.append(" AND action = ?");
                params.add(action);
            }
            if (hasEntityType) {
                sql.append(" AND entity_type = ?");
                params.add(entityType);
            }
            if (startDate != null) {
                sql.append(" AND created_at >= ?");
                params.add(Timestamp.valueOf(startDate.atStartOfDay()));
            }
            if (endDate != null) {
                sql.append(" AND created_at <= ?");
                params.add(Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            }
        }
    }
}
