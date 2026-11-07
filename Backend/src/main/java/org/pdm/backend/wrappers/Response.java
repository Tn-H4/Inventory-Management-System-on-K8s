package org.pdm.backend.wrappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.model.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    private int status;
    private String message;

    private String token;
    private UserRole role;
    private String expirationTime;

    private Integer totalPages;
    private Long totalElements;

    // data output
    private User user;
    private List<User> users;

    private Supplier supplier;
    private List<Supplier> suppliers;

    private Category category;
    private List<Category> categories;

    private Product product;
    private List<Product> products;

    private Transaction transaction;
    private List<Transaction> transactions;

    private Warehouse warehouse;
    private List<Warehouse> warehouses;

    private ProductSupplier productSupplier;
    private List<ProductSupplier> productSuppliers;

    private List<Notification> notifications;
    private Long unreadCount;

    private Proposal proposal;
    private List<Proposal> proposals;
    private AuditLog auditLog;
    private List<AuditLog> auditLogs;
    private Long pendingProposalsCount;
    private DashboardStats dashboardStats;

    private final LocalDateTime timestamp = LocalDateTime.now();
}
