package org.pdm.backend.repository;

import org.pdm.backend.model.AuditLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {
    /**
     * Saves a new audit log entry.
     *
     * @param auditLog The audit log to save.
     * @return The saved AuditLog object.
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Retrieves an audit log entry by its ID.
     *
     * @param id The ID of the audit log.
     * @return An Optional containing the audit log if found.
     */
    Optional<AuditLog> findById(Long id);

    /**
     * Retrieves a filtered, paginated list of audit logs.
     */
    List<AuditLog> findAllPaged(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate,
            String sortBy, String sortDir,
            int page, int size
    );

    /**
     * Counts audit logs matching the given filters.
     */
    long countFiltered(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate
    );
}
