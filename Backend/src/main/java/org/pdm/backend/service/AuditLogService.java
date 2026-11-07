package org.pdm.backend.service;

import org.pdm.backend.wrappers.Response;

import java.time.LocalDate;

public interface AuditLogService {
    /**
     * Retrieves a filtered, paginated list of all audit logs.
     */
    Response getAllAuditLogs(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate,
            String sortBy, String sortDir,
            int page, int size
    );

    /**
     * Retrieves a specific audit log entry by its ID.
     *
     * @param id The ID of the audit log entry.
     * @return A Response containing the audit log details.
     * @throws org.pdm.backend.exception.NotFoundException if the audit log is not found.
     */
    Response getAuditLogById(Long id);

    /**
     * Logs a system action for auditing purposes.
     *
     * @param userId The ID of the user performing the action.
     * @param action The action performed.
     * @param entityType The type of entity affected.
     * @param entityId The ID of the affected entity.
     * @param details Additional details about the action.
     */
    void logAction(Long userId, String action, String entityType, Long entityId, String details);

    /**
     * Exports all audit logs to a CSV format.
     *
     * @return A CSV string containing the audit log data.
     */
    String exportAuditLogsCsv();
}
