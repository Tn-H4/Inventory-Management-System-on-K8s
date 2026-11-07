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
 * Integration tests for SupplierController (/api/suppliers/**).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL = "test.supplier.admin@test.local";

    private long supplierId;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.insertTestUser("Supplier Admin", ADMIN_EMAIL, ENCODER.encode("admin123"), "ADMIN");

        supplierId = TestUtils.insertTestSupplier("Supplier Test Supplier " + System.currentTimeMillis());

        adminToken = TestUtils.loginAndGetToken(mockMvc, ADMIN_EMAIL, "admin123");
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteSupplierById(supplierId);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
    }

    // -----------------------------------------------------------------------
    // GET /api/suppliers/all
    // -----------------------------------------------------------------------

    @Test
    void getAllSuppliers_withValidToken_returns200AndSuppliersArray() throws Exception {
        mockMvc.perform(get("/api/suppliers/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.suppliers").isArray());
    }

    @Test
    void getAllSuppliers_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/suppliers/all"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // GET /api/suppliers/{id}
    // -----------------------------------------------------------------------

    @Test
    void getSupplierById_withValidToken_returns200AndCorrectSupplier() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + supplierId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.supplier").exists())
                .andExpect(jsonPath("$.supplier.id").value(supplierId));
    }

    @Test
    void getSupplierById_forNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/suppliers/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // GET /api/suppliers/search
    // -----------------------------------------------------------------------

    @Test
    void searchSuppliers_withMatchingInput_returns200AndResults() throws Exception {
        mockMvc.perform(get("/api/suppliers/all")
                        .param("input", "Supplier Test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").isArray())
                .andExpect(jsonPath("$.suppliers", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void searchSuppliers_withNoMatchingInput_returns200AndEmptyList() throws Exception {
        mockMvc.perform(get("/api/suppliers/all")
                        .param("input", "xyzzy_nonexistent_supplier_12345")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").isArray())
                .andExpect(jsonPath("$.suppliers", hasSize(0)));
    }

    // -----------------------------------------------------------------------
    // POST /api/suppliers/add (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void addSupplier_withAdminTokenAndValidData_returns200() throws Exception {
        String uniqueName = "New Supplier " + System.currentTimeMillis();
        String requestBody = """
                {
                    "name": "%s",
                    "address": "123 Test Street",
                    "contactInfo": "+84 900 000 001",
                    "email": "%s@test.local",
                    "website": "https://%s.test.com",
                    "taxCode": "1234567890",
                    "status": "ACTIVE"
                }
                """.formatted(uniqueName, uniqueName.toLowerCase().replace(" ", ""), uniqueName.toLowerCase().replace(" ", ""));

        mockMvc.perform(post("/api/suppliers/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // Cleanup - find and delete the supplier we just created
        deleteSupplierByName(uniqueName);
    }

    @Test
    void addSupplier_withNoToken_returns401() throws Exception {
        String requestBody = """
                {
                    "name": "No Auth Supplier",
                    "address": "123 Test Street"
                }
                """;

        mockMvc.perform(post("/api/suppliers/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PUT /api/suppliers/update/{id} (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void updateSupplier_withAdminTokenAndValidData_returns200() throws Exception {
        String requestBody = """
                {
                    "name": "Updated Supplier Name",
                    "address": "456 New Street",
                    "contactInfo": "+84 900 000 002",
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(put("/api/suppliers/update/" + supplierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/suppliers/delete/{id} (ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void deleteSupplier_withValidId_returns200() throws Exception {
        // Create a supplier to delete
        long supplierToDelete = TestUtils.insertTestSupplier("To Delete " + System.currentTimeMillis());

        mockMvc.perform(delete("/api/suppliers/delete/" + supplierToDelete)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // No cleanup needed - we just deleted it
    }

    @Test
    void deleteSupplier_withInvalidId_returns404() throws Exception {
        mockMvc.perform(delete("/api/suppliers/delete/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void deleteSupplierByName(String name) throws Exception {
        try (var conn = org.pdm.backend.security.DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT id FROM suppliers WHERE name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    TestUtils.deleteSupplierById(rs.getLong(1));
                }
            }
        }
    }
}