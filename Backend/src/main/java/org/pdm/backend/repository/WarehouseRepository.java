package org.pdm.backend.repository;

import org.pdm.backend.model.Warehouse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository {
    /**
     * Retrieves all warehouses.
     *
     * @return A list of all warehouses.
     */
    List<Warehouse> findAll();
    /**
     * Retrieves a paginated and filtered list of warehouses.
     *
     * @param search Search query for warehouse name.
     * @param status Filter by warehouse status.
     * @param minCapacity Minimum capacity filter.
     * @param maxCapacity Maximum capacity filter.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of filtered and paginated warehouses.
     */
    List<Warehouse> findAllPaged(String search, String status, Integer minCapacity, Integer maxCapacity, String sortBy, String sortDir, int page, int size);
    /**
     * Counts the total number of warehouses matching the filters.
     *
     * @param search Search query for warehouse name.
     * @param status Filter by warehouse status.
     * @param minCapacity Minimum capacity filter.
     * @param maxCapacity Maximum capacity filter.
     * @return The total count of matching warehouses.
     */
    long countAll(String search, String status, Integer minCapacity, Integer maxCapacity);
    /**
     * Retrieves a warehouse by its ID.
     *
     * @param id The ID of the warehouse.
     * @return An Optional containing the warehouse if found.
     */
    Optional<Warehouse> findById(Long id);
    /**
     * Saves a new warehouse.
     *
     * @param warehouse The warehouse to save.
     * @return The saved Warehouse object.
     */
    Warehouse save(Warehouse warehouse);
    /**
     * Updates an existing warehouse.
     *
     * @param warehouse The warehouse to update.
     * @return The updated Warehouse object.
     */
    Warehouse update(Warehouse warehouse);
    /**
     * Deletes a warehouse by its ID (soft delete).
     *
     * @param id The ID of the warehouse to delete.
     * @return The number of affected rows.
     */
    Long deleteById(Long id);
}
