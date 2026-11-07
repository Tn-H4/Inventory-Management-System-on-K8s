package org.pdm.backend.repository;

import org.pdm.backend.enums.TransactionStatus;
import org.pdm.backend.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository {
    /**
     * Saves a new transaction.
     *
     * @param transaction The transaction to save.
     * @return The saved Transaction object.
     */
    Transaction save(Transaction transaction);
    /**
     * Updates an existing transaction.
     *
     * @param transaction The transaction to update.
     * @return The updated Transaction object.
     */
    Transaction update(Transaction transaction);
    /**
     * Retrieves a transaction by its ID.
     *
     * @param id The ID of the transaction.
     * @return An Optional containing the transaction if found.
     */
    Optional<Transaction> findById(Long id);
    /**
     * Retrieves all transactions for a specific month and year.
     *
     * @param month The month (1-12).
     * @param year The year.
     * @return A list of transactions.
     */
    List<Transaction> findAllByMonthAndYear(int month, int year);
    /**
     * Searches for transactions with basic pagination.
     *
     * @param page The page number.
     * @param size The number of records per page.
     * @param searchText The search query.
     * @return A list of matching transactions.
     */
    List<Transaction> searchTransactions( int page, int size,String searchText);
    /**
     * Retrieves a paginated and filtered list of transactions.
     *
     * @param filter Search query.
     * @param status Filter by transaction status.
     * @param page The page number.
     * @param size The number of records per page.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A list of filtered and paginated transactions.
     */
    List<Transaction> findAllFilteredPaged(String filter, String status, int page, int size, String sortBy, String sortDir);
    /**
     * Counts the total number of transactions matching the filters.
     *
     * @param filter Search query.
     * @param status Filter by status.
     * @return The total count of matching transactions.
     */
    long countFiltered(String filter, String status);

    /**
     * Updates the status of a transaction.
     *
     * @param id The ID of the transaction.
     * @param status The new status to set.
     * @return The updated Transaction object.
     */
    Transaction updateStatus(Long id, TransactionStatus status);
    /**
     * Nullifies the product ID for all transactions associated with a deleted product.
     *
     * @param productId The ID of the product.
     */
    void nullifyProductId(Long productId);

}
