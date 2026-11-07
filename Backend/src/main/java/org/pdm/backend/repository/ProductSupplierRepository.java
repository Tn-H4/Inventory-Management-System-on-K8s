package org.pdm.backend.repository;

import org.pdm.backend.model.ProductSupplier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplierRepository {
    /**
     * Retrieves all suppliers for a given product.
     *
     * @param productId The ID of the product.
     * @return A list of product-supplier links.
     */
    List<ProductSupplier> findByProductId(Long productId);

    /**
     * Retrieves all products for a given supplier.
     *
     * @param supplierId The ID of the supplier.
     * @return A list of product-supplier links.
     */
    List<ProductSupplier> findBySupplierId(Long supplierId);

    /**
     * Retrieves all product-supplier links.
     *
     * @return A list of all product-supplier links.
     */
    List<ProductSupplier> findAll();

    /**
     * Saves a new product-supplier link.
     *
     * @param productSupplier The link to save.
     * @return The saved ProductSupplier object.
     */
    ProductSupplier save(ProductSupplier productSupplier);

    /**
     * Updates an existing product-supplier link.
     *
     * @param productSupplier The link to update.
     * @return The updated ProductSupplier object.
     */
    ProductSupplier update(ProductSupplier productSupplier);

    /**
     * Deletes the link between a specific product and supplier.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     */
    void deleteByProductIdAndSupplierId(Long productId, Long supplierId);

    /**
     * Deletes all supplier links for a specific product.
     *
     * @param productId The ID of the product.
     */
    void deleteByProductId(Long productId);

    /**
     * Retrieves a specific product-supplier link.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @return An Optional containing the link if found.
     */
    Optional<ProductSupplier> findByProductIdAndSupplierId(Long productId, Long supplierId);

    /**
     * Removes the preferred status from all suppliers for a given product.
     *
     * @param productId The ID of the product.
     */
    void clearPreferredForProduct(Long productId);

    /**
     * Removes the preferred status from all suppliers for a product except for one.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier to keep as preferred.
     */
    void clearPreferredForProductExcept(Long productId, Long supplierId);

    /**
     * Updates the tracked purchase and return quantities for a product-supplier link.
     *
     * @param productId The ID of the product.
     * @param supplierId The ID of the supplier.
     * @param purchasedDelta The amount to add to total purchased.
     * @param returnedDelta The amount to add to total returned.
     */
    void updatePurchaseReturnQuantities(Long productId, Long supplierId, Long purchasedDelta, Long returnedDelta);
}
