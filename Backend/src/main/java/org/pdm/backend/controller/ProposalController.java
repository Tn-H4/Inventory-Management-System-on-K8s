package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.service.ProposalService;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "Proposal management endpoints for approval workflow")
public class ProposalController {

    private final ProposalService proposalService;

    @GetMapping("/all")
    @Operation(summary = "Get all proposals", description = "Retrieves paginated proposals (MANAGER/ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Proposals retrieved successfully")
    public Response getAllProposals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return proposalService.getAllProposals(page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get proposal by ID", description = "Retrieves a specific proposal by ID")
    @ApiResponse(responseCode = "200", description = "Proposal retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public Response getProposalById(@PathVariable Long id) {
        return proposalService.getProposalById(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MANAGER')")
    @Operation(summary = "Approve proposal", description = "Approves a pending proposal and executes the transaction (MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Proposal approved successfully")
    @ApiResponse(responseCode = "400", description = "Proposal is not pending")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER required")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public Response approveProposal(@PathVariable Long id, @RequestBody(required = false) String reviewNote) {
        return proposalService.approveProposal(id, reviewNote);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MANAGER')")
    @Operation(summary = "Reject proposal", description = "Rejects a pending proposal (MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Proposal rejected successfully")
    @ApiResponse(responseCode = "400", description = "Proposal is not pending")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER required")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public Response rejectProposal(@PathVariable Long id, @RequestBody(required = false) String reviewNote) {
        return proposalService.rejectProposal(id, reviewNote);
    }

    @GetMapping("/pending-count")
    @Operation(summary = "Get pending proposals count", description = "Retrieves the count of pending proposals")
    @ApiResponse(responseCode = "200", description = "Pending count retrieved successfully")
    public Response getPendingCount() {
        return proposalService.getPendingCount();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('MANAGER')")
    @Operation(summary = "Export proposals to CSV", description = "Exports proposals as a CSV file (MANAGER only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER required")
    public ResponseEntity<String> exportProposalsCsv() {
        String csv = proposalService.exportProposalsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=proposals.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
