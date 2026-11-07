package org.pdm.backend.service;

import org.pdm.backend.model.ProductSupplier;
import org.pdm.backend.wrappers.Response;

public interface ProductSupplierService {
    /**
     * Retrieves all suppliers associated with a specific product.
     *
     * @param productId The ID of the product.
     * @return A Response containing the list of suppliers.
     */
    Response getSuppliersByProduct(Long productId);

    /**
     * Retrieves all products supplied by a specific supplier.
     *
     * @param supplierId The ID of the supplier.
     * @return A Response containing the list of products.
     */
    Response getProductsBySupplier(Long supplierId);

    /**
     * Retrieves the link details between a specific product and supplier.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @return A Response containing the product-supplier relationship.
     */
    Response getProductSupplierLink(Long productId, Long supplierId);

    /**
     * Retrieves all product-supplier associations.
     *
     * @return A Response containing the list of all product-supplier links.
     */
    Response getAllProductSuppliers();

    /**
     * Adds a new association between a product and a supplier.
     *
     * @param productSupplier The product-supplier association to add.
     * @return A Response indicating the result of the operation.
     */
    Response addProductSupplier(ProductSupplier productSupplier);

    /**
     * Updates an existing product-supplier association.
     *
     * @param productSupplier The product-supplier association to update.
     * @return A Response indicating the result of the operation.
     */
    Response updateProductSupplier(ProductSupplier productSupplier);

    /**
     * Removes the association between a product and a supplier.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @return A Response indicating the result of the operation.
     */
    Response removeProductSupplier(Long productId, Long supplierId);
}
