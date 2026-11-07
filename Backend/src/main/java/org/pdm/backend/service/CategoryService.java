package org.pdm.backend.service;

import org.pdm.backend.model.Category;
import org.pdm.backend.wrappers.Response;

public interface CategoryService {

    /**
     * Creates a new category.
     *
     * @param category The category details to create.
     * @return A Response indicating the result of the category creation.
     */
    Response createCategory(Category category);

    /**
     * Retrieves a paginated and filtered list of all categories.
     *
     * @param page The page number to retrieve.
     * @param limit The number of records per page.
     * @param search Search query for category name or description.
     * @param description Filter by description.
     * @param status Filter by category status.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A Response containing the paginated list of categories.
     */
    Response getAllCategories(int page, int limit, String search, String description, String status, String sortBy, String sortDir);

    /**
     * Retrieves a specific category by its ID.
     *
     * @param id The ID of the category.
     * @return A Response containing the category details.
     * @throws org.pdm.backend.exception.NotFoundException if the category is not found.
     */
    Response getCategoryById(Long id);

    /**
     * Updates an existing category.
     *
     * @param id The ID of the category to update.
     * @param categoryDetails The updated category details.
     * @return A Response indicating the result of the category update.
     * @throws org.pdm.backend.exception.NotFoundException if the category is not found.
     */
    Response updateCategory(Long id, Category categoryDetails);

    /**
     * Deletes a category by its ID (soft delete).
     *
     * @param id The ID of the category to delete.
     * @return A Response indicating the result of the deletion.
     * @throws org.pdm.backend.exception.NotFoundException if the category is not found.
     */
    Response deleteCategory(Long id);

    /**
     * Toggles the status of a category between active and inactive.
     *
     * @param id The ID of the category.
     * @return A Response indicating the result of the status toggle.
     * @throws org.pdm.backend.exception.NotFoundException if the category is not found.
     */
    Response toggleCategoryStatus(Long id);

    /**
     * Exports a filtered list of categories to a CSV format.
     *
     * @param search Search query.
     * @return A CSV string containing the category data.
     */
    String exportCategoriesCsv(String search);
}