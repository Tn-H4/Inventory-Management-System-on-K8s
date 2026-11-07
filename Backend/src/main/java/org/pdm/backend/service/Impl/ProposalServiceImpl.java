package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.util.CsvExportUtils;
import org.pdm.backend.enums.NotificationType;
import org.pdm.backend.enums.ProposalStatus;
import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.enums.TransactionType;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Product;
import org.pdm.backend.model.Proposal;
import org.pdm.backend.model.Transaction;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.ProposalRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.repository.TransactionRepository;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.service.AuditLogService;
import org.pdm.backend.service.NotificationService;
import org.pdm.backend.service.ProposalService;
import org.pdm.backend.service.StockNotificationService;
import org.pdm.backend.service.UserService;
import org.pdm.backend.wrappers.Response;
import org.pdm.backend.wrappers.TransactionRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalServiceImpl implements ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final StockNotificationService stockNotificationService;
    private final AuditLogService auditLogService;

    /**
     * Creates a proposal for a transaction and logs it in the system.
     *
     * @param request The transaction request containing proposal details.
     * @return A Response indicating the result of the proposal creation.
     */
    @Override
    public Response createProposal(TransactionRequest request) {
        User user = userService.getCurrentLoggedInUser();

        Proposal proposal = new Proposal();
        proposal.setTransactionType(request.getTransactionType());
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setProductId(request.getProductId());
        proposal.setSupplierId(request.getSupplierId());
        proposal.setQuantity(request.getQuantity());
        proposal.setDescription(request.getDescription());
        proposal.setProposedBy(user.getId());

        // Calculate total price for display
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId()).orElse(null);
            if (product != null && product.getPrice() != null) {
                proposal.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            }
        }

        proposal = proposalRepository.save(proposal);

        // Create a PENDING transaction so the STAFF has a paper trail on the transactions page
        Transaction pendingTx = new Transaction();
        pendingTx.setTransactionType(proposal.getTransactionType());
        pendingTx.setStatus(TransactionStatus.PENDING);
        pendingTx.setProductId(proposal.getProductId());
        pendingTx.setSupplierId(proposal.getSupplierId());
        pendingTx.setUserId(user.getId());
        pendingTx.setTotalProducts(proposal.getQuantity());
        pendingTx.setTotalPrice(proposal.getTotalPrice());
        pendingTx.setDescription(proposal.getDescription());
        pendingTx.setNote("Proposal #" + proposal.getId() + " — pending approval");
        pendingTx.setCreatedAt(LocalDateTime.now());
        Transaction savedTx = transactionRepository.save(pendingTx);

        proposalRepository.updateTransactionId(proposal.getId(), savedTx.getId());
        proposal.setTransactionId(savedTx.getId());

        // Log the proposal creation
        auditLogService.logAction(user.getId(), "PROPOSAL_CREATED", "proposal", proposal.getId(),
                "Created " + request.getTransactionType() + " proposal for quantity " + request.getQuantity());

        notifyIncomingProposal(proposal);

        return Response.builder()
                .status(200)
                .message("Proposal submitted for approval")
                .proposal(proposal)
                .build();
    }

    /**
     * Retrieves a paginated list of all proposals.
     *
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of proposals.
     */
    @Override
    public Response getAllProposals(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        List<Proposal> proposals = proposalRepository.findAllPaged(safePage, safeSize, sortBy, sortDir);
        long totalElements = proposalRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return Response.builder()
                .status(200)
                .message("success")
                .proposals(proposals)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Retrieves a specific proposal by its ID.
     *
     * @param id The ID of the proposal.
     * @return A Response containing the proposal details.
     * @throws NotFoundException if the proposal is not found.
     */
    @Override
    public Response getProposalById(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proposal Not Found"));

        return Response.builder()
                .status(200)
                .message("success")
                .proposal(proposal)
                .build();
    }

    /**
     * Approves a proposal, executes the associated transaction, and notifies the proposer.
     *
     * @param id The ID of the proposal to approve.
     * @param reviewNote Note from the reviewer.
     * @return A Response indicating the result.
     * @throws NotFoundException if the proposal is not found.
     */
    @Override
    public Response approveProposal(Long id, String reviewNote) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proposal Not Found"));

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            return Response.builder()
                    .status(400)
                    .message("Proposal is not pending")
                    .build();
        }

        User reviewer = userService.getCurrentLoggedInUser();

        // Execute the transaction
        executeProposal(proposal);

        // Update proposal status
        proposalRepository.updateStatus(id, ProposalStatus.APPROVED.name(), reviewer.getId(), reviewNote);

        // Log approval
        auditLogService.logAction(reviewer.getId(), "PROPOSAL_APPROVED", "proposal", id,
                "Approved " + proposal.getTransactionType() + " proposal with quantity " + proposal.getQuantity());

        notifyProposalDecision(proposal, ProposalStatus.APPROVED);

        return Response.builder()
                .status(200)
                .message("Proposal approved and executed")
                .build();
    }

    /**
     * Rejects a proposal and updates the linked transaction status to CANCELLED.
     *
     * @param id The ID of the proposal to reject.
     * @param reviewNote Note from the reviewer.
     * @return A Response indicating the result.
     * @throws NotFoundException if the proposal is not found.
     */
    @Override
    public Response rejectProposal(Long id, String reviewNote) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proposal Not Found"));

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            return Response.builder()
                    .status(400)
                    .message("Proposal is not pending")
                    .build();
        }

        User reviewer = userService.getCurrentLoggedInUser();
        proposalRepository.updateStatus(id, ProposalStatus.REJECTED.name(), reviewer.getId(), reviewNote);

        updateLinkedTransactionStatus(proposal, TransactionStatus.CANCELLED);

        // Log rejection
        auditLogService.logAction(reviewer.getId(), "PROPOSAL_REJECTED", "proposal", id,
                "Rejected " + proposal.getTransactionType() + " proposal with quantity " + proposal.getQuantity());

        notifyProposalDecision(proposal, ProposalStatus.REJECTED);

        return Response.builder()
                .status(200)
                .message("Proposal rejected")
                .build();
    }

    /**
     * Exports all proposals to a CSV format.
     *
     * @return A CSV string containing the proposal data.
     */
    @Override
    public String exportProposalsCsv() {
        List<Proposal> proposals = proposalRepository.findAllPaged(1, 10_000, "created_at", "DESC");
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Type,Status,Product Name,Supplier ID,Quantity,Total Price,Proposed By,Reviewed By,Review Note,Created At,Reviewed At\n");
        for (Proposal p : proposals) {
            csv.append(p.getId()).append(",");
            csv.append(p.getTransactionType() != null ? p.getTransactionType() : "").append(",");
            csv.append(p.getStatus() != null ? p.getStatus() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(p.getProduct() != null ? p.getProduct().getName() : "")).append(",");
            csv.append(p.getSupplierId() != null ? p.getSupplierId() : "").append(",");
            csv.append(p.getQuantity() != null ? p.getQuantity() : "").append(",");
            csv.append(p.getTotalPrice() != null ? p.getTotalPrice() : "").append(",");
            csv.append(p.getProposedByUser() != null ? CsvExportUtils.escapeCsv(p.getProposedByUser().getName()) : CsvExportUtils.escapeCsv(p.getProposedBy() != null ? String.valueOf(p.getProposedBy()) : "")).append(",");
            csv.append(p.getReviewedByUser() != null ? CsvExportUtils.escapeCsv(p.getReviewedByUser().getName()) : CsvExportUtils.escapeCsv(p.getReviewedBy() != null ? String.valueOf(p.getReviewedBy()) : "")).append(",");
            csv.append(CsvExportUtils.escapeCsv(p.getReviewNote())).append(",");
            csv.append(CsvExportUtils.isoDateTime(p.getCreatedAt())).append(",");
            csv.append(CsvExportUtils.isoDateTime(p.getReviewedAt())).append("\n");
        }
        return csv.toString();
    }

    /**
     * Retrieves the count of proposals currently in PENDING status.
     *
     * @return A Response containing the pending proposals count.
     */
    @Override
    public Response getPendingCount() {
        long count = proposalRepository.countPending();

        return Response.builder()
                .status(200)
                .message("success")
                .pendingProposalsCount(count)
                .build();
    }

    private void updateLinkedTransactionStatus(Proposal proposal, TransactionStatus status) {
        if (proposal.getTransactionId() != null) {
            transactionRepository.updateStatus(proposal.getTransactionId(), status);
        }
    }

    private void executeProposal(Proposal proposal) {
        User approver = userService.getCurrentLoggedInUser();
        TransactionType type = proposal.getTransactionType();

        if (type == TransactionType.PURCHASE || type == TransactionType.SALE) {
            Product product = productRepository.findById(proposal.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            int currentStock = getCurrentStock(product);

            // Validate sufficient stock for SALE transactions
            if (type == TransactionType.SALE && currentStock < proposal.getQuantity()) {
                throw new org.pdm.backend.exception.NameValueRequiredException(
                        "Insufficient stock. Available: " + currentStock);
            }

            Integer previousStockQuantity = product.getStockQuantity();
            if (type == TransactionType.PURCHASE) {
                product.setStockQuantity(currentStock + proposal.getQuantity());
            } else {
                product.setStockQuantity(currentStock - proposal.getQuantity());
            }

            productRepository.update(product);
            stockNotificationService.checkAndNotify(product, previousStockQuantity);

            updateLinkedTransactionStatus(proposal, TransactionStatus.COMPLETED);

            // Log transaction
            auditLogService.logAction(approver.getId(), "TRANSACTION_EXECUTED", "transaction", proposal.getTransactionId(),
                    "Executed " + type + " for product " + product.getName() + " quantity " + proposal.getQuantity());

        } else if (type == TransactionType.RETURN_TO_SUPPLIER) {
            // Validate supplier ID exists
            if (proposal.getSupplierId() == null) {
                throw new org.pdm.backend.exception.NameValueRequiredException("Supplier Id is Required");
            }

            Product product = productRepository.findById(proposal.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            supplierRepository.findById(proposal.getSupplierId())
                    .orElseThrow(() -> new NotFoundException("Supplier Not Found"));

            int currentStock = getCurrentStock(product);

            // Validate sufficient stock for return
            if (currentStock < proposal.getQuantity()) {
                throw new org.pdm.backend.exception.NameValueRequiredException(
                        "Insufficient stock. Available: " + currentStock);
            }

            Integer previousStockQuantity = product.getStockQuantity();
            product.setStockQuantity(currentStock - proposal.getQuantity());
            productRepository.update(product);
            stockNotificationService.checkAndNotify(product, previousStockQuantity);

            updateLinkedTransactionStatus(proposal, TransactionStatus.COMPLETED);

            // Log return
            auditLogService.logAction(approver.getId(), "RETURN_EXECUTED", "transaction", proposal.getTransactionId(),
                    "Executed RETURN_TO_SUPPLIER for product " + product.getName() + " quantity " + proposal.getQuantity());
        }
    }

    private void notifyIncomingProposal(Proposal proposal) {
        String title = "Proposal awaiting decision";
        String message = buildProposalMessage(proposal, "submitted");
        for (User manager : getManagers()) {
            notificationService.createNotification(
                    manager.getId(),
                    title,
                    message,
                    NotificationType.INFO,
                    "PROPOSAL_INCOMING",
                    "PROPOSAL",
                    proposal.getId(),
                    "/proposals/" + proposal.getId(),
                    "proposal:" + proposal.getId() + ":incoming"
            );
        }
    }

    private void notifyProposalDecision(Proposal proposal, ProposalStatus status) {
        String decision = status == ProposalStatus.APPROVED ? "approved" : "rejected";
        NotificationType type = status == ProposalStatus.APPROVED ? NotificationType.SUCCESS : NotificationType.INFO;
        String title = "Proposal " + decision;
        String message = buildProposalMessage(proposal, decision);

        if (proposal.getProposedBy() != null) {
            notificationService.createNotification(
                    proposal.getProposedBy(),
                    title,
                    message,
                    type,
                    status == ProposalStatus.APPROVED ? "PROPOSAL_APPROVED" : "PROPOSAL_REJECTED",
                    "PROPOSAL",
                    proposal.getId(),
                    "/proposals/" + proposal.getId(),
                    "proposal:" + proposal.getId() + ":" + status.name().toLowerCase()
            );
        }
    }

    private List<User> getManagers() {
        return userRepository.findByRole(UserRole.MANAGER).stream()
                .filter(user -> user.getId() != null)
                .toList();
    }

    private int getCurrentStock(Product product) {
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }

    private String buildProposalMessage(Proposal proposal, String action) {
        return String.format(
                "%s proposal #%d was %s for %d unit(s).",
                proposal.getTransactionType(),
                proposal.getId(),
                action,
                proposal.getQuantity()
        );
    }
}
