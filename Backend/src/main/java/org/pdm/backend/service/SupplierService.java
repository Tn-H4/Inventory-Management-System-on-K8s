package org.pdm.backend.service;

import org.pdm.backend.model.Supplier; 
import org.pdm.backend.wrappers.Response;

public interface SupplierService {
    /**
     * Retrieves a paginated and filtered list of all suppliers.
     *
     * @param input Search query for supplier name or tax code.
     * @param status Filter by supplier status.
     * @param taxCode Filter by specific tax code.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number to retrieve.
     * @param size The number of records per page.
     * @return A Response containing the paginated list of suppliers.
     */
    Response getAllSuppliers(String input, String status, String taxCode, String sortBy, String sortDir, int page, int size);

    /**
     * Retrieves a specific supplier by its ID.
     *
     * @param id The ID of the supplier.
     * @return A Response containing the supplier details.
     * @throws org.pdm.backend.exception.NotFoundException if the supplier is not found.
     */
    Response getSupplierById(Long id);

    /**
     * Creates a new supplier.
     *
     * @param supplier The supplier details to create.
     * @return A Response indicating the result of the supplier creation.
     */
    Response createSupplier(Supplier supplier);

    /**
     * Updates an existing supplier.
     *
     * @param id The ID of the supplier to update.
     * @param supplier The updated supplier details.
     * @return A Response indicating the result of the supplier update.
     * @throws org.pdm.backend.exception.NotFoundException if the supplier is not found.
     */
    Response updateSupplier(Long id, Supplier supplier);

    /**
     * Deletes a supplier by its ID (soft delete).
     *
     * @param id The ID of the supplier to delete.
     * @return A Response indicating the result of the deletion.
     * @throws org.pdm.backend.exception.NotFoundException if the supplier is not found.
     */
    Response deleteSupplier(Long id);

    /**
     * Toggles the status of a supplier between active and inactive.
     *
     * @param id The ID of the supplier.
     * @return A Response indicating the result of the status toggle.
     * @throws org.pdm.backend.exception.NotFoundException if the supplier is not found.
     */
    Response toggleSupplierStatus(Long id);

    /**
     * Exports a filtered list of suppliers to a CSV format.
     *
     * @param input Search query.
     * @param status Filter by status.
     * @return A CSV string containing the supplier data.
     */
    String exportSuppliersCsv(String input, String status);
}
