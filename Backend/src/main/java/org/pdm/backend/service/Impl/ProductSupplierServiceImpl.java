package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.ProductSupplier;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.ProductSupplierRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.service.ProductSupplierService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierRepository productSupplierRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Retrieves all suppliers associated with a specific product.
     *
     * @param productId The ID of the product.
     * @return A Response containing the list of product-supplier links.
     * @throws NotFoundException if the product is not found.
     */
    @Override
    public Response getSuppliersByProduct(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
        List<ProductSupplier> links = productSupplierRepository.findByProductId(productId);
        return Response.builder()
                .status(200)
                .message("Success")
                .productSuppliers(links)
                .build();
    }

    /**
     * Retrieves all products associated with a specific supplier.
     *
     * @param supplierId The ID of the supplier.
     * @return A Response containing the list of product-supplier links.
     * @throws NotFoundException if the supplier is not found.
     */
    @Override
    public Response getProductsBySupplier(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + supplierId));
        List<ProductSupplier> links = productSupplierRepository.findBySupplierId(supplierId);
        return Response.builder()
                .status(200)
                .message("Success")
                .productSuppliers(links)
                .build();
    }

    /**
     * Retrieves a specific product-supplier link.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @return A Response containing the product-supplier link.
     */
    @Override
    public Response getProductSupplierLink(Long productId, Long supplierId) {
        ProductSupplier link = productSupplierRepository.findByProductIdAndSupplierId(productId, supplierId)
                .orElse(null);
        return Response.builder()
                .status(200)
                .message("Success")
                .productSupplier(link)
                .build();
    }

    /**
     * Retrieves all product-supplier links in the system.
     *
     * @return A Response containing the full list of links.
     */
    @Override
    public Response getAllProductSuppliers() {
        List<ProductSupplier> links = productSupplierRepository.findAll();
        return Response.builder()
                .status(200)
                .message("Success")
                .productSuppliers(links)
                .build();
    }

    /**
     * Creates a new link between a product and a supplier.
     *
     * @param productSupplier The link details.
     * @return A Response indicating the result.
     * @throws NotFoundException if the product or supplier is not found.
     * @throws IllegalArgumentException if the link already exists.
     */
    @Override
    public Response addProductSupplier(ProductSupplier productSupplier) {
        productRepository.findById(productSupplier.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productSupplier.getProductId()));
        supplierRepository.findById(productSupplier.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + productSupplier.getSupplierId()));

        productSupplierRepository.findByProductIdAndSupplierId(
                        productSupplier.getProductId(), productSupplier.getSupplierId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Link between product and supplier already exists");
                });

        boolean hasPreferredSupplier = productSupplierRepository.findByProductId(productSupplier.getProductId())
                .stream()
                .anyMatch(link -> Boolean.TRUE.equals(link.getIsPreferred()));
        boolean shouldBePreferred = Boolean.TRUE.equals(productSupplier.getIsPreferred()) || !hasPreferredSupplier;
        productSupplier.setIsPreferred(shouldBePreferred);

        if (shouldBePreferred) {
            productSupplierRepository.clearPreferredForProduct(productSupplier.getProductId());
        }

        ProductSupplier saved = productSupplierRepository.save(productSupplier);
        return Response.builder()
                .status(200)
                .message("Product-supplier link created successfully")
                .productSuppliers(List.of(saved))
                .build();
    }

    /**
     * Updates an existing product-supplier link.
     *
     * @param productSupplier The updated link details.
     * @return A Response indicating the result.
     * @throws NotFoundException if the link is not found.
     */
    @Override
    public Response updateProductSupplier(ProductSupplier productSupplier) {
        productSupplierRepository.findByProductIdAndSupplierId(
                        productSupplier.getProductId(), productSupplier.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Product-supplier link not found"));

        if (Boolean.TRUE.equals(productSupplier.getIsPreferred())) {
            productSupplierRepository.clearPreferredForProductExcept(
                    productSupplier.getProductId(), productSupplier.getSupplierId());
        }

        ProductSupplier updated = productSupplierRepository.update(productSupplier);
        return Response.builder()
                .status(200)
                .message("Product-supplier link updated successfully")
                .productSuppliers(List.of(updated))
                .build();
    }

    /**
     * Removes the link between a product and a supplier.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @return A Response indicating the result.
     * @throws NotFoundException if the link is not found.
     */
    @Override
    public Response removeProductSupplier(Long productId, Long supplierId) {
        productSupplierRepository.findByProductIdAndSupplierId(productId, supplierId)
                .orElseThrow(() -> new NotFoundException("Product-supplier link not found"));
        productSupplierRepository.deleteByProductIdAndSupplierId(productId, supplierId);
        return Response.builder()
                .status(200)
                .message("Product-supplier link removed successfully")
                .build();
    }
}
