package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;

import org.pdm.backend.enums.SupplierStatus;
import org.pdm.backend.util.CsvExportUtils;
import org.pdm.backend.exception.NameValueRequiredException;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Supplier;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.service.SupplierService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository supplierRepository;

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
     * @throws NameValueRequiredException if the provided status is invalid.
     */
    @Override
    public Response getAllSuppliers(String input, String status, String taxCode, String sortBy, String sortDir, int page, int size) {

        if (status != null && !status.isBlank()) {
            try {
                SupplierStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new NameValueRequiredException("Invalid supplier status: " + status);
            }
        }

        List<Supplier> suppliers = supplierRepository.findAllPaged(input, status, taxCode, sortBy, sortDir, page, size);
        int total = supplierRepository.countAll(input, status, taxCode);
        int totalPages = (int) Math.ceil((double) total / size);

        return Response.builder()
                .status(200)
                .message("Success")
                .suppliers(suppliers)
                .totalElements((long) total)
                .totalPages(totalPages)
                .build();
    }
    /**
     * Retrieves a specific supplier by its ID.
     *
     * @param id The ID of the supplier.
     * @return A Response containing the supplier details.
     * @throws NotFoundException if the supplier is not found.
     */
    @Override
    public Response getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER NOT FOUND WITH ID: " + id));
        return Response.builder()
                .status(200)
                .message("Success")
                .supplier(supplier) 
                .build();

    }
    /**
     * Creates a new supplier.
     *
     * @param savedSupplier The supplier details to create.
     * @return A Response indicating the result of the supplier creation.
     * @throws NameValueRequiredException if the supplier name is missing.
     */
    @Override
    public Response createSupplier(Supplier savedSupplier){
        if (savedSupplier.getName() == null || savedSupplier.getName().isBlank()){
            throw new NameValueRequiredException("Supplier name is required");
        }
        supplierRepository.save(savedSupplier);
        return Response.builder()
                .status(200)
                .message("SUCCESSED!")
                .supplier(savedSupplier)
                .build();
    }
    /**
     * Updates an existing supplier's details.
     *
     * @param id The ID of the supplier to update.
     * @param supplierRequest The updated supplier details.
     * @return A Response indicating the result of the update.
     * @throws NotFoundException if the supplier is not found.
     */
    @Override
    public Response updateSupplier(Long id, Supplier supplierRequest) {
        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER NOT FOUND WITH ID: " + id));
        if (supplierRequest.getName() != null && !supplierRequest.getName().isBlank()) {
            existingSupplier.setName(supplierRequest.getName());
        }
        if (supplierRequest.getContactInfo() != null) {
            existingSupplier.setContactInfo(supplierRequest.getContactInfo());
        }
        if (supplierRequest.getAddress() != null) {
            existingSupplier.setAddress(supplierRequest.getAddress());
        }
        if (supplierRequest.getEmail() != null) {
            existingSupplier.setEmail(supplierRequest.getEmail());
        }
        if (supplierRequest.getWebsite() != null) {
            existingSupplier.setWebsite(supplierRequest.getWebsite());
        }
        if (supplierRequest.getTaxCode() != null) {
            existingSupplier.setTaxCode(supplierRequest.getTaxCode());
        }
        if (supplierRequest.getStatus() != null) {
            existingSupplier.setStatus(supplierRequest.getStatus());
        }
        Supplier updatedSupplier = supplierRepository.update(existingSupplier);

        return Response.builder()
                .status(200)
                .message("Supplier updated successfully")
                .supplier(updatedSupplier)
                .build();
    }
    /**
     * Deletes a supplier by its ID (soft delete).
     *
     * @param id The ID of the supplier to delete.
     * @return A Response indicating the result of the deletion.
     * @throws NotFoundException if the supplier is not found.
     */
    @Override
    public Response deleteSupplier(Long id) {
        supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));
        supplierRepository.deleteById(id);
        return Response.builder()
                .status(200)
                .message("Supplier deleted successfully")
                .build();
    }

    /**
     * Exports a filtered list of suppliers to a CSV format.
     *
     * @param input Search query.
     * @param status Filter by status.
     * @return A CSV string containing the supplier data.
     */
    @Override
    public String exportSuppliersCsv(String input, String status) {
        List<Supplier> suppliers = supplierRepository.findAllPaged(input, status, null, null, "ASC", 1, 10_000);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Status,Contact Info,Email,Website,Tax Code,Address,Created At\n");
        for (Supplier s : suppliers) {
            csv.append(s.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getName())).append(",");
            csv.append(s.getStatus() != null ? s.getStatus() : "").append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getContactInfo())).append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getEmail())).append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getWebsite())).append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getTaxCode())).append(",");
            csv.append(CsvExportUtils.escapeCsv(s.getAddress())).append(",");
            csv.append(CsvExportUtils.isoDateTime(s.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    /**
     * Toggles the status of a supplier between ACTIVE and INACTIVE.
     *
     * @param id The ID of the supplier.
     * @return A Response indicating the result of the status toggle.
     * @throws NotFoundException if the supplier is not found.
     */
    @Override
    public Response toggleSupplierStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));

        SupplierStatus currentStatus = supplier.getStatus();
        SupplierStatus newStatus = (currentStatus == SupplierStatus.ACTIVE)
                ? SupplierStatus.INACTIVE
                : SupplierStatus.ACTIVE;

        supplier.setStatus(newStatus);
        Supplier updated = supplierRepository.update(supplier);

        return Response.builder()
                .status(200)
                .message("Supplier status toggled to " + newStatus)
                .supplier(updated)
                .build();
    }
}
