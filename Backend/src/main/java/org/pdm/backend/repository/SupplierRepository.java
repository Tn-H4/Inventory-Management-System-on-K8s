package org.pdm.backend.repository;

import org.pdm.backend.model.Supplier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface SupplierRepository {
    /**
     * Retrieves a supplier by its ID.
     *
     * @param id The ID of the supplier.
     * @return An Optional containing the supplier if found.
     */
    Optional<Supplier> findById(Long id);
    /**
     * Retrieves all suppliers.
     *
     * @return A list of all suppliers.
     */
    List<Supplier> findAll();
    /**
     * Retrieves a paginated and filtered list of suppliers.
     *
     * @param input Search query for name or tax code.
     * @param status Filter by status.
     * @param taxCode Filter by specific tax code.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of filtered and paginated suppliers.
     */
    List<Supplier> findAllPaged(String input, String status, String taxCode, String sortBy, String sortDir, int page, int size);
    /**
     * Counts the total number of suppliers matching the filters.
     *
     * @param input Search query for name or tax code.
     * @param status Filter by status.
     * @param taxCode Filter by specific tax code.
     * @return The total count of matching suppliers.
     */
    int countAll(String input, String status, String taxCode);
    /**
     * Saves a new supplier.
     *
     * @param supplierToSave The supplier to save.
     * @return The saved Supplier object.
     */
    Supplier save(Supplier supplierToSave);
    /**
     * Updates an existing supplier.
     *
     * @param supplierToUpdate The supplier to update.
     * @return The updated Supplier object.
     */
    Supplier update(Supplier supplierToUpdate);
    /**
     * Deletes a supplier by its ID (soft delete).
     *
     * @param id The ID of the supplier to delete.
     * @return The number of affected rows.
     */
    Long deleteById(Long id);
    /**
     * Searches for suppliers by any available field.
     *
     * @param input The search query.
     * @return A list of matching suppliers.
     */
    List<Supplier> searchSupplierByAnything(String input);
}

