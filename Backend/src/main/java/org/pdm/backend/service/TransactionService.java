package org.pdm.backend.service;

import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.wrappers.Response;
import org.pdm.backend.wrappers.TransactionRequest;

public interface TransactionService {
    /**
     * Restocks inventory through a purchase transaction.
     *
     * @param transactionRequest The request details for restocking.
     * @return A Response indicating the result of the restocking operation.
     */
    Response restockInventory(TransactionRequest transactionRequest);

    /**
     * Records a sale transaction and reduces inventory stock.
     *
     * @param transactionRequest The request details for the sale.
     * @return A Response indicating the result of the sale operation.
     */
    Response sell(TransactionRequest transactionRequest);

    /**
     * Returns products to a supplier and reduces inventory stock.
     *
     * @param transactionRequest The request details for returning products.
     * @return A Response indicating the result of the return-to-supplier operation.
     */
    Response returnToSupplier(TransactionRequest transactionRequest);

    /**
     * Retrieves a paginated and filtered list of all transactions.
     *
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @param filter Search query or filter for transactions.
     * @param status Filter by transaction status.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of transactions.
     */
    Response getAllTransactions(int page, int size, String filter, String status, String sortBy, String sortDir);

    /**
     * Retrieves a specific transaction by its ID.
     *
     * @param id The ID of the transaction.
     * @return A Response containing the transaction details.
     * @throws org.pdm.backend.exception.NotFoundException if the transaction is not found.
     */
    Response getTransactionById(Long id);

    /**
     * Retrieves all transactions for a specific month and year.
     *
     * @param month The month (1-12).
     * @param year The year.
     * @return A Response containing the transactions for the specified period.
     */
    Response getAllTransactionByMonthAndYear(int month, int year);

    /**
     * Updates the status of an existing transaction.
     *
     * @param transactionId The ID of the transaction to update.
     * @param transactionStatus The new status for the transaction.
     * @return A Response indicating the result of the status update.
     * @throws org.pdm.backend.exception.NotFoundException if the transaction is not found.
     */
    Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus);

    /**
     * Exports a filtered list of transactions to a CSV format.
     *
     * @param filter Search query or filter for transactions.
     * @return A CSV string containing the transaction data.
     */
    String exportTransactionsCsv(String filter);
}
