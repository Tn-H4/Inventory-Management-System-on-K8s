package org.pdm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdm.backend.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CategoryController (/api/categories/**).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL = "test.category.admin@test.local";

    private long categoryId;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.insertTestUser("Category Admin", ADMIN_EMAIL, ENCODER.encode("admin123"), "ADMIN");

        categoryId = TestUtils.insertTestCategory("Category Test Category " + System.currentTimeMillis());

        adminToken = TestUtils.loginAndGetToken(mockMvc, ADMIN_EMAIL, "admin123");
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteCategoryById(categoryId);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
    }

    // -----------------------------------------------------------------------
    // GET /api/categories/all
    // -----------------------------------------------------------------------

    @Test
    void getAllCategories_withValidToken_returns200AndCategoriesArray() throws Exception {
        mockMvc.perform(get("/api/categories/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getAllCategories_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/categories/all"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // GET /api/categories/{id}
    // -----------------------------------------------------------------------

    @Test
    void getCategoryById_withValidToken_returns200AndCorrectCategory() throws Exception {
        mockMvc.perform(get("/api/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.category.id").value(categoryId));
    }

    @Test
    void getCategoryById_forNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/categories/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // GET /api/categories/search
    // -----------------------------------------------------------------------

    @Test
    void searchCategories_withMatchingInput_returns200AndResults() throws Exception {
        mockMvc.perform(get("/api/categories/all")
                        .param("search", "Category Test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void searchCategories_withNoMatchingInput_returns200AndEmptyList() throws Exception {
        mockMvc.perform(get("/api/categories/all")
                        .param("search", "xyzzy_nonexistent_category_12345")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    // -----------------------------------------------------------------------
    // POST /api/categories/add (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void addCategory_withAdminTokenAndValidData_returns200() throws Exception {
        String uniqueName = "New Category " + System.currentTimeMillis();
        String requestBody = """
                {
                    "name": "%s",
                    "description": "Created by integration test",
                    "isActive": true,
                    "displayOrder": 0
                }
                """.formatted(uniqueName);

        mockMvc.perform(post("/api/categories/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // Cleanup - find and delete the category we just created
        deleteCategoryByName(uniqueName);
    }

    @Test
    void addCategory_withNoToken_returns401() throws Exception {
        String requestBody = """
                {
                    "name": "No Auth Category",
                    "description": "Should not be created"
                }
                """;

        mockMvc.perform(post("/api/categories/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PUT /api/categories/update/{id} (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void updateCategory_withAdminTokenAndValidData_returns200() throws Exception {
        String requestBody = """
                {
                    "name": "Updated Category Name",
                    "description": "Updated description",
                    "isActive": false,
                    "displayOrder": 5
                }
                """;

        mockMvc.perform(put("/api/categories/update/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void updateCategory_withInvalidId_returns404() throws Exception {
        String requestBody = """
                {
                    "name": "Nonexistent Category"
                }
                """;

        mockMvc.perform(put("/api/categories/update/999999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/categories/delete/{id} (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void deleteCategory_withValidId_returns200() throws Exception {
        // Create a category to delete
        long categoryToDelete = TestUtils.insertTestCategory("To Delete " + System.currentTimeMillis());

        mockMvc.perform(delete("/api/categories/delete/" + categoryToDelete)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // No cleanup needed - we just deleted it
    }

    @Test
    void deleteCategory_withInvalidId_returns404() throws Exception {
        mockMvc.perform(delete("/api/categories/delete/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void deleteCategoryByName(String name) throws Exception {
        try (var conn = org.pdm.backend.security.DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT id FROM categories WHERE name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    TestUtils.deleteCategoryById(rs.getLong(1));
                }
            }
        }
    }
}