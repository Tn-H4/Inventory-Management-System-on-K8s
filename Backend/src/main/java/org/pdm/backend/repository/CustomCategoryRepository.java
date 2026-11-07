package org.pdm.backend.repository;

import java.util.List;

import org.pdm.backend.model.Category;

public interface CustomCategoryRepository {

    /**
     * Retrieves categories that have at least the specified minimum number of products.
     *
     * @param minProducts The minimum number of products a category must have to be included.
     * @return A list of categories meeting the criteria.
     */
    List<Category> findCategoriesWithManyProducts(int minProducts);

}
 