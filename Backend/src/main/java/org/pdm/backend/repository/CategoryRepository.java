package org.pdm.backend.repository;

import org.pdm.backend.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    /**
     * Saves or updates a category.
     *
     * @param category The category to save.
     * @return The saved Category object.
     */
    Category save(Category category);

    /**
     * Retrieves a category by its ID.
     *
     * @param id The ID of the category.
     * @return An Optional containing the category if found.
     */
    Optional<Category> findById(Long id);

    /**
     * Retrieves all categories.
     *
     * @return A list of all categories.
     */
    List<Category> findAll();

    /**
     * Deletes a category by its ID (soft delete).
     *
     * @param id The ID of the category to delete.
     */
    void deleteById(Long id);

    /**
     * Retrieves a category by its name.
     *
     * @param name The name of the category.
     * @return The category if found, otherwise null.
     */
    Category findByName(String name);

    /**
     * Retrieves a paginated and filtered list of categories.
     *
     * @param filter Search query for name.
     * @param description Filter by description.
     * @param status Filter by status.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number.
     * @param size The number of records per page.
     * @return A list of filtered and paginated categories.
     */
    List<Category> findAllFilteredPaged(String filter, String description, String status, String sortBy, String sortDir, int page, int size);

    /**
     * Counts the total number of categories matching the filters.
     *
     * @param filter Search query for name.
     * @param description Filter by description.
     * @param status Filter by status.
     * @return The total count of matching categories.
     */
    long countFiltered(String filter, String description, String status);
}