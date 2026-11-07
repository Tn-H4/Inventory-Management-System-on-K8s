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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProductController (/api/products/**).
 *
 * NOTE: @Transactional/@Rollback have no effect — see AuthControllerTest.
 *
 * NOTE: POST /api/products/add uses multipart/form-data (not JSON).
 * The service saves the image file to {cwd}/../frontend/public/products/ as a side effect.
 * That directory is created automatically; the test image file is a 1x1 minimal JPEG.
 *
 * NOTE: GET /api/products/all has NO server-side pagination — the backend ignores
 * page and limit params and returns all matching products.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL   = "test.prod.admin@test.local";
    private static final String MANAGER_EMAIL = "test.prod.manager@test.local";

    private long categoryId;
    private long productId;
    private String adminToken;
    private String managerToken;

    /** Minimal valid JPEG bytes (1x1 pixel). Satisfies imageFile.getContentType().startsWith("image/"). */
    private static final byte[] MINIMAL_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9
    };

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);

        TestUtils.insertTestUser("Prod Admin",   ADMIN_EMAIL,   ENCODER.encode("admin123"),   "ADMIN");
        TestUtils.insertTestUser("Prod Manager", MANAGER_EMAIL, ENCODER.encode("manager123"), "MANAGER");

        categoryId = TestUtils.insertTestCategory("Test Category " + System.currentTimeMillis());
        productId  = TestUtils.insertTestProduct(
                "Test Product A", "TST-SKU-" + System.currentTimeMillis(),
                50, 10, categoryId);

        adminToken   = TestUtils.loginAndGetToken(mockMvc, ADMIN_EMAIL,   "admin123");
        managerToken = TestUtils.loginAndGetToken(mockMvc, MANAGER_EMAIL, "manager123");
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteProductById(productId);
        TestUtils.deleteCategoryById(categoryId);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
    }

    // -----------------------------------------------------------------------
    // GET /api/products/all
    // -----------------------------------------------------------------------

    @Test
    void getAllProducts_withValidToken_returns200AndProductsArray() throws Exception {
        mockMvc.perform(get("/api/products/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getAllProducts_withCategoryFilter_returnsOnlyMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/products/all")
                        .param("categoryId", String.valueOf(categoryId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                // every returned product must belong to our test category
                .andExpect(jsonPath("$.products[*].categoryId",
                        everyItem(is((int) categoryId))));
    }

    @Test
    void getAllProducts_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // -----------------------------------------------------------------------
    // GET /api/products/{id}
    // -----------------------------------------------------------------------

    @Test
    void getProductById_withValidToken_returns200AndCorrectProduct() throws Exception {
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.product").exists())
                .andExpect(jsonPath("$.product.id").value(productId));
    }

    @Test
    void getProductById_forNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/products/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // GET /api/products/search
    // -----------------------------------------------------------------------

    @Test
    void searchProducts_withMatchingInput_returns200AndResults() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("input", "Test Product")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void searchProducts_withNoMatchingInput_returns404() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("input", "xyzzy_definitely_not_a_product_name_12345")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/products/add  (multipart/form-data, ADMIN only)
    // -----------------------------------------------------------------------

    @Test
    void addProduct_withAdminTokenAndValidForm_returns200() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", "image/jpeg", MINIMAL_JPEG);

        String uniqueName = "New Integration Product " + System.currentTimeMillis();

        mockMvc.perform(multipart("/api/products/add")
                        .file(imageFile)
                        .param("name",          uniqueName)
                        .param("price",         "500000")
                        .param("stockQuantity", "20")
                        .param("categoryId",    String.valueOf(categoryId))
                        .param("description",   "Created by integration test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        org.junit.jupiter.api.Assertions.assertTrue(findSkuByName(uniqueName).matches("^SKU\\d{6}$"));

        // Cleanup: the product was created without us knowing its ID.
        deleteProductByName(uniqueName);
    }

    @Test
    void addProduct_withManagerTokenAndValidForm_returns200() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", "image/jpeg", MINIMAL_JPEG);
        String uniqueName = "Manager Integration Product " + System.currentTimeMillis();

        mockMvc.perform(multipart("/api/products/add")
                        .file(imageFile)
                        .param("name",          uniqueName)
                        .param("price",         "100000")
                        .param("stockQuantity", "5")
                        .param("categoryId",    String.valueOf(categoryId))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        deleteProductByName(uniqueName);
    }

    @Test
    void addProduct_withNoToken_returns401() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "test.jpg", "image/jpeg", MINIMAL_JPEG);

        mockMvc.perform(multipart("/api/products/add")
                        .file(imageFile)
                        .param("name",          "No Auth Product")
                        .param("price",         "100000")
                        .param("stockQuantity", "5")
                        .param("categoryId",    String.valueOf(categoryId)))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void deleteProductByName(String name) throws Exception {
        try (var conn = org.pdm.backend.security.DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT id FROM products WHERE name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    TestUtils.deleteProductById(rs.getLong(1));
                }
            }
        }
    }

    private String findSkuByName(String name) throws Exception {
        try (var conn = org.pdm.backend.security.DatabaseConfig.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT sku FROM products WHERE name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        throw new AssertionError("Created product not found: " + name);
    }
}
