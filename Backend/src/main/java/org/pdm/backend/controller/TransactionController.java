package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.service.TransactionService;
import org.pdm.backend.wrappers.Response;
import org.pdm.backend.wrappers.TransactionRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping("/purchase")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'STAFF')")
    @Operation(summary = "Purchase stock", description = "Create a purchase transaction to restock inventory (MANAGER) or submit proposal (STAFF)")
    @ApiResponse(responseCode = "200", description = "Purchase transaction created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER/STAFF required")
    public ResponseEntity<Response> restockInventory(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.restockInventory(transactionRequest));
    }
    @PostMapping("/sell")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'STAFF')")
    @Operation(summary = "Sell products", description = "Create a sell transaction to reduce inventory (MANAGER) or submit proposal (STAFF)")
    @ApiResponse(responseCode = "200", description = "Sell transaction created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER/STAFF required")
    public ResponseEntity<Response> sell(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.sell(transactionRequest));
    }
    @PostMapping("/return")
    @PreAuthorize("hasAuthority('MANAGER')")
    @Operation(summary = "Return to supplier", description = "Create a return transaction to return products to supplier (MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Return transaction created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "403", description = "Access denied - MANAGER required")
    public ResponseEntity<Response> returnToSupplier(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.returnToSupplier(transactionRequest));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all transactions", description = "Retrieves paginated list of transactions with optional filters and sorting")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully")
    public ResponseEntity<Response> getAllTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String status,
            @RequestParam(value = "sortBy", defaultValue = "created_at") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir
    ) {
        // Pass 1-indexed page directly - repository handles offset conversion (page - 1) * size
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size, filter, status, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a single transaction by its ID")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    public ResponseEntity<Response> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/by-month-year")
    @Operation(summary = "Get transactions by month and year", description = "Retrieves transactions for a specific month and year")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully")
    public ResponseEntity<Response> getAllTransactionByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactionByMonthAndYear(month, year));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update transaction status", description = "Updates the status of a transaction (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "Transaction status updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN/MANAGER required")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestBody @Valid TransactionStatus status) {
        return ResponseEntity.ok(transactionService.updateTransactionStatus(transactionId, status));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Export transactions to CSV", description = "Exports transactions as a CSV file (ADMIN/MANAGER only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN or MANAGER required")
    public ResponseEntity<String> exportTransactionsCsv(
            @RequestParam(required = false) String filter) {
        String csv = transactionService.exportTransactionsCsv(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }



}
