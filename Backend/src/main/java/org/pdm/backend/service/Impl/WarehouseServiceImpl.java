package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.util.CsvExportUtils;
import org.pdm.backend.model.Warehouse;
import org.pdm.backend.repository.WarehouseRepository;
import org.pdm.backend.service.WarehouseService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    /**
     * Retrieves a paginated list of warehouses based on search filters and sorting criteria.
     *
     * @param search      The search keyword for warehouse name or location.
     * @param status      The filter for active/inactive warehouses.
     * @param minCapacity The minimum capacity filter.
     * @param maxCapacity The maximum capacity filter.
     * @param sortBy      The field to sort by.
     * @param sortDir      The sorting direction (ASC or DESC).
     * @param page        The page number (0-indexed).
     * @param limit       The number of items per page.
     * @return A Response object containing the list of warehouses and pagination metadata.
     */
    public Response getAllWarehouses(String search, String status, Integer minCapacity, Integer maxCapacity, String sortBy, String sortDir, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        List<Warehouse> warehouses = warehouseRepository.findAllPaged(search, status, minCapacity, maxCapacity, sortBy, sortDir, safePage, safeLimit);
        long total = warehouseRepository.countAll(search, status, minCapacity, maxCapacity);
        int totalPages = (int) Math.ceil((double) total / safeLimit);

        return Response.builder()
                .status(200)
                .message("Success")
                .warehouses(warehouses)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    @Override
    /**
     * Retrieves a warehouse by its unique identifier.
     *
     * @param id The unique identifier of the warehouse.
     * @return A Response object containing the found warehouse.
     * @throws NotFoundException if no warehouse is found with the given id.
     */
    public Response getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));
        return Response.builder()
                .status(200)
                .message("Success")
                .warehouse(warehouse)
                .build();
    }

    @Override
    /**
     * Creates a new warehouse in the system.
     *
     * @param warehouse The warehouse details to create.
     * @return A Response object containing the created warehouse.
     * @throws IllegalArgumentException if required fields (name, location) are missing.
     */
    public Response createWarehouse(Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
        if (warehouse.getLocation() == null || warehouse.getLocation().isBlank()) {
            throw new IllegalArgumentException("Warehouse location is required");
        }
        Warehouse saved = warehouseRepository.save(warehouse);
        return Response.builder()
                .status(200)
                .message("Warehouse created successfully")
                .warehouse(saved)
                .build();
    }

    @Override
    /**
     * Updates an existing warehouse's details.
     *
     * @param id               The unique identifier of the warehouse to update.
     * @param warehouseRequest The updated warehouse details.
     * @return A Response object containing the updated warehouse.
     * @throws NotFoundException if no warehouse is found with the given id.
     */
    public Response updateWarehouse(Long id, Warehouse warehouseRequest) {
        Warehouse existing = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

        if (warehouseRequest.getName() != null && !warehouseRequest.getName().isBlank()) {
            existing.setName(warehouseRequest.getName());
        }
        if (warehouseRequest.getLocation() != null && !warehouseRequest.getLocation().isBlank()) {
            existing.setLocation(warehouseRequest.getLocation());
        }
        if (warehouseRequest.getAddress() != null) {
            existing.setAddress(warehouseRequest.getAddress());
        }
        if (warehouseRequest.getCapacity() != null) {
            existing.setCapacity(warehouseRequest.getCapacity());
        }
        if (warehouseRequest.getManagerName() != null) {
            existing.setManagerName(warehouseRequest.getManagerName());
        }
        if (warehouseRequest.getIsActive() != null) {
            existing.setIsActive(warehouseRequest.getIsActive());
        }

        Warehouse updated = warehouseRepository.update(existing);
        return Response.builder()
                .status(200)
                .message("Warehouse updated successfully")
                .warehouse(updated)
                .build();
    }

    @Override
    /**
     * Deletes a warehouse from the system.
     *
     * @param id The unique identifier of the warehouse to delete.
     * @return A Response object indicating successful deletion.
     * @throws NotFoundException if no warehouse is found with the given id.
     */
    public Response deleteWarehouse(Long id) {
        warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));
        warehouseRepository.deleteById(id);
        return Response.builder()
                .status(200)
                .message("Warehouse deleted successfully")
                .build();
    }

    @Override
    /**
     * Exports a list of warehouses to a CSV string.
     *
     * @return A CSV formatted string containing warehouse data.
     */
    public String exportWarehousesCsv() {
        List<Warehouse> warehouses = warehouseRepository.findAll().stream()
                .limit(10_000)
                .toList();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Location,Address,Capacity,Manager Name,Is Active,Created At\n");
        for (Warehouse w : warehouses) {
            csv.append(w.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(w.getName())).append(",");
            csv.append(CsvExportUtils.escapeCsv(w.getLocation())).append(",");
            csv.append(CsvExportUtils.escapeCsv(w.getAddress())).append(",");
            csv.append(w.getCapacity() != null ? w.getCapacity() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(w.getManagerName())).append(",");
            csv.append(w.getIsActive() != null ? w.getIsActive() : "").append(",");
            csv.append(CsvExportUtils.isoDateTime(w.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    @Override
    /**
     * Toggles the active status of a warehouse.
     *
     * @param id The unique identifier of the warehouse.
     * @return A Response object containing the warehouse with the updated status.
     * @throws NotFoundException if no warehouse is found with the given id.
     */
    public Response toggleWarehouseStatus(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

        Boolean currentStatus = warehouse.getIsActive();
        Boolean newStatus = (currentStatus == null || currentStatus == true) ? false : true;

        warehouse.setIsActive(newStatus);
        warehouseRepository.update(warehouse);

        return Response.builder()
                .status(200)
                .message("Warehouse status toggled to " + (newStatus ? "ACTIVE" : "INACTIVE"))
                .warehouse(warehouse)
                .build();
    }
}
