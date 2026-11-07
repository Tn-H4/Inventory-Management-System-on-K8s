package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.pdm.backend.model.User;
import org.pdm.backend.service.UserService;
import org.pdm.backend.wrappers.AdminCreateUserRequest;
import org.pdm.backend.wrappers.ChangePasswordRequest;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {


    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieves paginated list of users with optional filters (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<Response> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getAllUsers(search, role, status, startDate, endDate, sortBy, sortDir, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a single user by its ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Response> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update user", description = "Updates user information")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Response> updateUser(@PathVariable Long id, @RequestBody User userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft deletes a user (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "User deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<Response> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @PutMapping("/activate/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Activate user", description = "Reactivates a deactivated user (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "User reactivated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<Response> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.reactivateUser(id));
    }

    @GetMapping("/transactions/{userId}")
    @Operation(summary = "Get user with transactions", description = "Retrieves a user along with their transaction history")
    @ApiResponse(responseCode = "200", description = "User and transactions retrieved successfully")
    public ResponseEntity<Response> getUserAndTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserTransactions(userId));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current user", description = "Retrieves the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user retrieved successfully")
    public ResponseEntity<User> getCurrentUser(){
        return ResponseEntity.ok(userService.getCurrentLoggedInUser());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new user", description = "Creates a new user account (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<Response> createUser(@RequestBody @Valid AdminCreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/change-password/{id}")
    @Operation(summary = "Change user password", description = "Updates the password for a user")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Current password is incorrect")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<Response> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword()));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Export users to CSV", description = "Exports users as a CSV file (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - ADMIN required")
    public ResponseEntity<String> exportUsersCsv() {
        String csv = userService.exportUsersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

}