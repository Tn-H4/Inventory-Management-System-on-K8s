package org.pdm.backend.service.Impl;

import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.enums.TransactionType;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.exception.NameValueRequiredException;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Product;
import org.pdm.backend.model.ProductSupplier;
import org.pdm.backend.model.Supplier;
import org.pdm.backend.model.Transaction;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.ProductSupplierRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.repository.TransactionRepository;
import org.pdm.backend.service.AuditLogService;
import org.pdm.backend.service.ProposalService;
import org.pdm.backend.service.StockNotificationService;
import org.pdm.backend.service.TransactionService;
import org.pdm.backend.service.UserService;
import org.pdm.backend.wrappers.Response;
import org.pdm.backend.wrappers.TransactionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.util.CsvExportUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

        private final TransactionRepository transactionRepository;
        private final SupplierRepository supplierRepository;
        private final UserService userService;
        private final ProductRepository productRepository;
        private final ProductSupplierRepository productSupplierRepository;
        private final StockNotificationService stockNotificationService;
        private final ProposalService proposalService;
        private final AuditLogService auditLogService;

        /**
         * Executes a stock replenishment (PURCHASE) transaction.
         * STAFF users will have their request converted into a proposal.
         *
         * @param transactionRequest The transaction details.
         * @return A Response indicating the result.
         * @throws NotFoundException if the product or supplier is not found.
         * @throws NameValueRequiredException if required fields are missing.
         */
        @Override
        public Response restockInventory(TransactionRequest transactionRequest) {

            Long productId = transactionRequest.getProductId();
            Long supplierId = transactionRequest.getSupplierId();
            Integer quantity = transactionRequest.getQuantity();

            if (supplierId == null)
                throw new NameValueRequiredException("Supplier Id is Required");

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new NotFoundException("Supplier Not Found"));

            User user = userService.getCurrentLoggedInUser();

            // STAFF must create a proposal instead of executing directly
            if (user.getRole() == UserRole.STAFF) {
                transactionRequest.setTransactionType(TransactionType.PURCHASE);
                return proposalService.createProposal(transactionRequest);
            }

            // Track purchase quantity for this supplier
            ProductSupplier productSupplier = productSupplierRepository.findByProductIdAndSupplierId(productId, supplierId)
                    .orElse(null);
            BigDecimal unitCost = BigDecimal.ZERO;
            if (productSupplier == null) {
                // Create new product-supplier link with initial purchase quantity
                ProductSupplier newPs = ProductSupplier.builder()
                        .productId(productId)
                        .supplierId(supplierId)
                        .isPreferred(false)
                        .build();
                productSupplierRepository.save(newPs);
                productSupplierRepository.updatePurchaseReturnQuantities(productId, supplierId, quantity.longValue(), 0L);
            } else {
                // Update existing tracking
                productSupplierRepository.updatePurchaseReturnQuantities(productId, supplierId, quantity.longValue(), 0L);
                unitCost = productSupplier.getUnitCost() != null ? productSupplier.getUnitCost() : BigDecimal.ZERO;
            }

            int currentStock = getCurrentStock(product);
            Integer previousStockQuantity = product.getStockQuantity();
            product.setStockQuantity(currentStock + quantity);
            productRepository.update(product);

            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.PURCHASE);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProductId(product.getId());
            transaction.setUserId(user.getId());
            transaction.setSupplierId(supplier.getId());
            transaction.setTotalProducts(quantity);
            transaction.setTotalPrice(unitCost.multiply(BigDecimal.valueOf(quantity)));
            transaction.setDescription(transactionRequest.getDescription());
            transaction.setCreatedAt(LocalDateTime.now());

            transactionRepository.save(transaction);
            stockNotificationService.checkAndNotify(product, previousStockQuantity);

            auditLogService.logAction(user.getId(), "PURCHASE", "transaction", transaction.getId(),
                    "Purchased " + quantity + " units of product " + product.getName());

            return Response.builder()
                    .status(200)
                    .message("Transaction Made Successfully")
                    .build();
        }

        /**
         * Executes a product sale transaction.
         * STAFF users will have their request converted into a proposal.
         *
         * @param transactionRequest The transaction details.
         * @return A Response indicating the result.
         * @throws NotFoundException if the product is not found.
         * @throws NameValueRequiredException if there is insufficient stock.
         */
        @Override
        public Response sell(TransactionRequest transactionRequest) {

            Long productId = transactionRequest.getProductId();
            Integer quantity = transactionRequest.getQuantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            // B5: Check for sufficient stock before selling
            assertSufficientStock(product, quantity);

            User user = userService.getCurrentLoggedInUser();

            // STAFF must create a proposal instead of executing directly
            if (user.getRole() == UserRole.STAFF) {
                transactionRequest.setTransactionType(TransactionType.SALE);
                return proposalService.createProposal(transactionRequest);
            }

            // update stock and resave
            int currentStock = getCurrentStock(product);
            Integer previousStockQuantity = product.getStockQuantity();
            product.setStockQuantity(currentStock - quantity);
            productRepository.update(product);

            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.SALE);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProductId(product.getId());
            transaction.setUserId(user.getId());
            transaction.setTotalProducts(quantity);
            transaction.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            transaction.setDescription(transactionRequest.getDescription());
            transaction.setCreatedAt(LocalDateTime.now());

            transactionRepository.save(transaction);
            stockNotificationService.checkAndNotify(product, previousStockQuantity);

            auditLogService.logAction(user.getId(), "SALE", "transaction", transaction.getId(),
                    "Sold " + quantity + " units of product " + product.getName());

            return Response.builder()
                    .status(200)
                    .message("Transaction Sold Successfully")
                    .build();
        }

        /**
         * Executes a product return to the supplier.
         *
         * @param transactionRequest The transaction details.
         * @return A Response indicating the result.
         * @throws NotFoundException if the product or supplier is not found.
         * @throws NameValueRequiredException if there is insufficient stock or no purchase history.
         */
        @Override
        public Response returnToSupplier(TransactionRequest transactionRequest) {

            Long productId = transactionRequest.getProductId();
            Long supplierId = transactionRequest.getSupplierId();
            Integer quantity = transactionRequest.getQuantity();

            if (supplierId == null)
                throw new NameValueRequiredException("Supplier Id is Required");

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new NotFoundException("Supplier Not Found"));

            User user = userService.getCurrentLoggedInUser();

            // Validate supplier-specific return quantity
            ProductSupplier productSupplier = productSupplierRepository.findByProductIdAndSupplierId(productId, supplierId)
                    .orElseThrow(() -> new NameValueRequiredException(
                            "No purchase history with this supplier. You must purchase from this supplier first."));

            long totalPurchased = productSupplier.getTotalPurchased() != null ? productSupplier.getTotalPurchased() : 0L;
            long totalReturned = productSupplier.getTotalReturned() != null ? productSupplier.getTotalReturned() : 0L;
            long availableToReturn = totalPurchased - totalReturned;

            if (quantity > availableToReturn) {
                throw new NameValueRequiredException(
                        "Cannot return " + quantity + " units. Available to return from this supplier: " + availableToReturn);
            }

            assertSufficientStock(product, quantity);

            if (user.getRole() == UserRole.STAFF) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Staff are not permitted to create return transactions");
            }

            BigDecimal unitCost = productSupplier.getUnitCost() != null ? productSupplier.getUnitCost() : BigDecimal.ZERO;

            int currentStock = getCurrentStock(product);
            Integer previousStockQuantity = product.getStockQuantity();
            product.setStockQuantity(currentStock - quantity);
            productRepository.update(product);

            // Track return quantity for this supplier
            productSupplierRepository.updatePurchaseReturnQuantities(productId, supplierId, 0L, quantity.longValue());

            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.RETURN_TO_SUPPLIER);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProductId(product.getId());
            transaction.setUserId(user.getId());
            transaction.setSupplierId(supplier.getId());
            transaction.setTotalProducts(quantity);
            transaction.setTotalPrice(unitCost.multiply(BigDecimal.valueOf(quantity)));
            transaction.setDescription(transactionRequest.getDescription());
            transaction.setCreatedAt(LocalDateTime.now());

            transactionRepository.save(transaction);
            stockNotificationService.checkAndNotify(product, previousStockQuantity);

            auditLogService.logAction(user.getId(), "RETURN_TO_SUPPLIER", "transaction", transaction.getId(),
                    "Returned " + quantity + " units of product " + product.getName() + " to supplier " + supplier.getName());

            return Response.builder()
                    .status(200)
                    .message("Transaction Returned Successfully Initialized")
                    .build();
        }

        /**
         * Retrieves a paginated and filtered list of all transactions.
         *
         * @param page The page number to retrieve.
         * @param size The number of records per page.
         * @param filter Search query.
         * @param status Filter by transaction status.
         * @param sortBy The field to sort by.
         * @param sortDir The direction of sorting (ASC or DESC).
         * @return A Response containing the paginated list of transactions.
         */
        @Override
        public Response getAllTransactions(int page, int size, String filter, String status, String sortBy, String sortDir) {

        // 1. Fetch paginated + filtered transactions
        List<Transaction> transactions =
                transactionRepository.findAllFilteredPaged(filter, status, page, size, sortBy, sortDir);

        // 2. Get count for pagination metadata
        long totalElements =
                transactionRepository.countFiltered(filter, status);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        // 3. Optional: Remove nested objects if needed (but keep product for display)
        transactions.forEach(t -> {
            t.setUserId(null);
            t.setSupplierId(null);
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactions)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

        /**
         * Retrieves a specific transaction by its ID.
         *
         * @param id The ID of the transaction.
         * @return A Response containing the transaction details.
         * @throws NotFoundException if the transaction is not found.
         */
        @Override
        public Response getTransactionById(Long id) {

            Transaction transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

            return Response.builder()
                    .status(200)
                    .message("success")
                    .transaction(transaction)
                    .build();
        }

        /**
         * Retrieves all transactions for a specific month and year.
         *
         * @param month The month (1-12).
         * @param year The year.
         * @return A Response containing the list of transactions.
         */
        @Override
        public Response getAllTransactionByMonthAndYear(int month, int year) {

            List<Transaction> transactions = transactionRepository.findAllByMonthAndYear(month, year);

            return Response.builder()
                    .status(200)
                    .message("success")
                    .transactions(transactions)
                    .build();
        }

        /**
         * Updates the status of a transaction.
         *
         * @param transactionId The ID of the transaction.
         * @param transactionStatus The new status to set.
         * @return A Response indicating the result.
         * @throws NotFoundException if the transaction is not found.
         */
        @Override
        public Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus) {

            Transaction existingTransaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

            existingTransaction.setStatus(transactionStatus);
            existingTransaction.setUpdatedAt(LocalDateTime.now());


            transactionRepository.updateStatus(transactionId, transactionStatus);

            return Response.builder()
                    .status(200)
                    .message("Transaction Status Successfully Updated")
                    .build();
        }

        /** Throws NameValueRequiredException if product stock is below the required quantity. */
        private void assertSufficientStock(Product product, int required) {
            int currentStock = getCurrentStock(product);
            if (currentStock < required) {
                throw new NameValueRequiredException(
                        "Insufficient stock. Available: " + currentStock);
            }
        }

        private int getCurrentStock(Product product) {
            return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        }

        /**
         * Exports filtered transactions to a CSV format.
         *
         * @param filter Search query.
         * @return A CSV string containing the transaction data.
         */
        @Override
        public String exportTransactionsCsv(String filter) {
            // Limit to 10 000 rows to avoid OOM on large tables
            final int EXPORT_LIMIT = 10_000;
            List<Transaction> transactions =
                    transactionRepository.findAllFilteredPaged(filter, null, 1, EXPORT_LIMIT, "created_at", "DESC");

            StringBuilder csv = new StringBuilder();
            csv.append("Date,Type,Status,Total Products,Total Price,Product Name,Description\n");

            for (Transaction t : transactions) {
                csv.append(CsvExportUtils.isoDateTime(t.getCreatedAt())).append(",");
                csv.append(t.getTransactionType()).append(",");
                csv.append(t.getStatus()).append(",");
                csv.append(t.getTotalProducts()).append(",");
                csv.append(t.getTotalPrice()).append(",");
                csv.append(CsvExportUtils.escapeCsv(t.getProduct() != null ? t.getProduct().getName() : "")).append(",");
                csv.append(CsvExportUtils.escapeCsv(t.getDescription() != null ? t.getDescription() : "")).append("\n");
            }

            return csv.toString();
        }

    }

