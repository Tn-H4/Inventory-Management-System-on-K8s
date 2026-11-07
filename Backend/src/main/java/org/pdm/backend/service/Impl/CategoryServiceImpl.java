package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Category;
import org.pdm.backend.repository.CategoryRepository;
import org.pdm.backend.service.CategoryService;
import org.pdm.backend.wrappers.Response;
import org.springframework.stereotype.Service;

import org.pdm.backend.util.CsvExportUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;



    /**
     * Creates a new category.
     *
     * @param category The category details to create.
     * @return A Response indicating the result of the category creation.
     */
    @Override
    public Response createCategory(Category category) {
        categoryRepository.save(category);

        return Response.builder()
                .status(200)
                .message("Category Saved Successfully")
                .build();

    }

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
    @Override
    public Response getAllCategories(int page, int limit, String search, String description, String status, String sortBy, String sortDir) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        // Fetch paginated + filtered categories
        List<Category> categoryList = categoryRepository.findAllFilteredPaged(search, description, status, sortBy, sortDir, safePage, safeLimit);

        // Get count for pagination metadata
        long totalElements = categoryRepository.countFiltered(search, description, status);
        int totalPages = (int) Math.ceil((double) totalElements / safeLimit);

        return Response.builder()
                .status(200)
                .message("success")
                .categories(categoryList)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Retrieves a specific category by its ID.
     *
     * @param id The ID of the category.
     * @return A Response containing the category details.
     * @throws NotFoundException if the category is not found.
     */
    @Override
    public Response getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));
        return Response.builder()
                .status(200)
                .message("success")
                .category(category)
                .build();
    }

    /**
     * Updates an existing category.
     *
     * @param id The ID of the category to update.
     * @param category The updated category details.
     * @return A Response indicating the result of the category update.
     * @throws NotFoundException if the category is not found.
     */
    @Override
    public Response updateCategory(Long id, Category category) {

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setImageUrl(category.getImageUrl());
        existingCategory.setIsActive(category.getIsActive());
        existingCategory.setDisplayOrder(category.getDisplayOrder());

        categoryRepository.save(existingCategory);

        return Response.builder()
                .status(200)
                .message("Category Was Successfully Updated")
                .build();

    }

    /**
     * Deletes a category by its ID (soft delete).
     *
     * @param id The ID of the category to delete.
     * @return A Response indicating the result of the deletion.
     * @throws NotFoundException if the category is not found.
     */
    @Override
    public Response deleteCategory(Long id) {

        categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        categoryRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Category Was Successfully Deleted")
                .build();
    }

    /**
     * Exports a filtered list of categories to a CSV format.
     *
     * @param search Search query.
     * @return A CSV string containing the category data.
     */
    @Override
    public String exportCategoriesCsv(String search) {
        List<Category> categories = categoryRepository.findAllFilteredPaged(search, null, null, null, "ASC", 1, 10_000);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Description,Is Active,Display Order,Created At\n");
        for (Category c : categories) {
            csv.append(c.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(c.getName())).append(",");
            csv.append(CsvExportUtils.escapeCsv(c.getDescription())).append(",");
            csv.append(c.getIsActive() != null ? c.getIsActive() : "").append(",");
            csv.append(c.getDisplayOrder() != null ? c.getDisplayOrder() : "").append(",");
            csv.append(CsvExportUtils.isoDateTime(c.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    /**
     * Toggles the status of a category between active and inactive.
     *
     * @param id The ID of the category.
     * @return A Response indicating the result of the status toggle.
     * @throws NotFoundException if the category is not found.
     */
    @Override
    public Response toggleCategoryStatus(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category Not Found"));

        Boolean currentStatus = category.getIsActive();
        Boolean newStatus = (currentStatus == null || currentStatus == true) ? false : true;

        category.setIsActive(newStatus);
        categoryRepository.save(category);

        return Response.builder()
                .status(200)
                .message("Category status toggled to " + (newStatus ? "ACTIVE" : "INACTIVE"))
                .category(category)
                .build();
    }
}