package org.pdm.backend.service;

import org.pdm.backend.model.Warehouse;
import org.pdm.backend.wrappers.Response;

public interface WarehouseService {
    /**
     * Retrieves a paginated and filtered list of all warehouses.
     *
     * @param search Search query for warehouse name.
     * @param status Filter by warehouse status.
     * @param minCapacity Minimum capacity filter.
     * @param maxCapacity Maximum capacity filter.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number to retrieve.
     * @param limit The number of records per page.
     * @return A Response containing the paginated list of warehouses.
     */
    Response getAllWarehouses(String search, String status, Integer minCapacity, Integer maxCapacity, String sortBy, String sortDir, int page, int limit);

    /**
     * Retrieves a specific warehouse by its ID.
     *
     * @param id The ID of the warehouse.
     * @return A Response containing the warehouse details.
     * @throws org.pdm.backend.exception.NotFoundException if the warehouse is not found.
     */
    Response getWarehouseById(Long id);

    /**
     * Creates a new warehouse.
     *
     * @param warehouse The warehouse details to create.
     * @return A Response indicating the result of the warehouse creation.
     */
    Response createWarehouse(Warehouse warehouse);

    /**
     * Updates an existing warehouse.
     *
     * @param id The ID of the warehouse to update.
     * @param warehouse The updated warehouse details.
     * @return A Response indicating the result of the warehouse update.
     * @throws org.pdm.backend.exception.NotFoundException if the warehouse is not found.
     */
    Response updateWarehouse(Long id, Warehouse warehouse);

    /**
     * Deletes a warehouse by its ID (soft delete).
     *
     * @param id The ID of the warehouse to delete.
     * @return A Response indicating the result of the deletion.
     * @throws org.pdm.backend.exception.NotFoundException if the warehouse is not found.
     */
    Response deleteWarehouse(Long id);

    /**
     * Toggles the status of a warehouse between active and inactive.
     *
     * @param id The ID of the warehouse.
     * @return A Response indicating the result of the status toggle.
     * @throws org.pdm.backend.exception.NotFoundException if the warehouse is not found.
     */
    Response toggleWarehouseStatus(Long id);

    /**
     * Exports all warehouses to a CSV format.
     *
     * @return A CSV string containing the warehouse data.
     */
    String exportWarehousesCsv();
}
