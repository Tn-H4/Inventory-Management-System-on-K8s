package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.service.AuditLogService;
import org.pdm.backend.wrappers.Response;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log management endpoints (ADMIN only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/all")
    @Operation(summary = "Get all audit logs", description = "Retrieves filtered, paginated audit logs (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
    public Response getAllAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogService.getAllAuditLogs(search, action, entityType, startDate, endDate, sortBy, sortDir, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID", description = "Retrieves a specific audit log (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Audit log retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Audit log not found")
    public Response getAuditLogById(@PathVariable Long id) {
        return auditLogService.getAuditLogById(id);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Export audit logs to CSV", description = "Exports audit logs as a CSV file (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<String> exportAuditLogsCsv() {
        String csv = auditLogService.exportAuditLogsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
