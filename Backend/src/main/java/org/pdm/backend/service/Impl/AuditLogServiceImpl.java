package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.util.CsvExportUtils;
import org.pdm.backend.model.AuditLog;
import org.pdm.backend.repository.AuditLogRepository;
import org.pdm.backend.service.AuditLogService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Retrieves a filtered, paginated list of all audit logs.
     */
    @Override
    public Response getAllAuditLogs(
            String search, String action, String entityType,
            LocalDate startDate, LocalDate endDate,
            String sortBy, String sortDir,
            int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        List<AuditLog> logs = auditLogRepository.findAllPaged(
                search, action, entityType, startDate, endDate, sortBy, sortDir, safePage, safeSize);
        long total = auditLogRepository.countFiltered(search, action, entityType, startDate, endDate);
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return Response.builder()
                .status(200)
                .message("success")
                .auditLogs(logs)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Retrieves a specific audit log entry by its ID.
     *
     * @param id The ID of the audit log entry.
     * @return A Response containing the audit log details.
     * @throws NotFoundException if the audit log is not found.
     */
    @Override
    public Response getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit Log Not Found"));

        return Response.builder()
                .status(200)
                .message("success")
                .auditLog(auditLog)
                .build();
    }

    /**
     * Exports all audit logs to a CSV format.
     *
     * @return A CSV string containing the audit log data.
     */
    @Override
    public String exportAuditLogsCsv() {
        List<AuditLog> logs = auditLogRepository.findAllPaged(null, null, null, null, null, "id", "ASC", 1, 20_000);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,User Name,User ID,Action,Entity Type,Entity ID,Details,Created At\n");
        for (AuditLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(log.getUser() != null ? log.getUser().getName() : "")).append(",");
            csv.append(log.getUserId() != null ? log.getUserId() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(log.getAction())).append(",");
            csv.append(CsvExportUtils.escapeCsv(log.getEntityType())).append(",");
            csv.append(log.getEntityId() != null ? log.getEntityId() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(log.getDetails())).append(",");
            csv.append(CsvExportUtils.isoDateTime(log.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    /**
     * Logs a system action for auditing purposes.
     *
     * @param userId The ID of the user performing the action.
     * @param action The action performed.
     * @param entityType The type of entity affected.
     * @param entityId The ID of the affected entity.
     * @param details Additional details about the action.
     */
    @Override
    public void logAction(Long userId, String action, String entityType, Long entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", e.getMessage());
        }
    }
}
