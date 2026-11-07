package org.pdm.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.service.Impl.ProductServiceImpl;
import org.pdm.backend.util.TestUtils;
import org.pdm.backend.wrappers.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProductServiceImpl.
 * Tests basic product operations.
 * Note: Some tests that rely on created_by/updated_by columns may fail if DB schema is not updated.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductServiceImpl productService;

    private long categoryId;
    private long productId;

    @BeforeEach
    void setUp() throws Exception {
        categoryId = TestUtils.insertTestCategory("Service Test Category " + System.currentTimeMillis());
        productId = TestUtils.insertTestProduct(
                "Service Test Product", "SVC-TST-" + System.currentTimeMillis(),
                50, 10, categoryId);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (productId > 0) {
            TestUtils.deleteProductById(productId);
        }
        if (categoryId > 0) {
            TestUtils.deleteCategoryById(categoryId);
        }
    }

    // -----------------------------------------------------------------------
    // getAllProducts tests
    // -----------------------------------------------------------------------

    @Test
    void getAllProducts_withNoFilters_returnsProductsWithPagination() {
        // Use page=1 to match controller's 1-indexed pagination
        Response response = productService.getAllProducts(null, null, null, null, null, null, 1, 20, "id", "ASC");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getProducts());
        assertNotNull(response.getTotalElements());
        assertNotNull(response.getTotalPages());
    }

    // -----------------------------------------------------------------------
    // searchProducts tests
    // -----------------------------------------------------------------------

    @Test
    void searchProducts_withMatchingInput_returnsProducts() {
        // Use product name for stable match
        Response response = productService.searchProducts("Service Test Product");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getProducts());
        assertTrue(response.getProducts().size() > 0);
    }

    @Test
    void searchProducts_withNoMatchingInput_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> {
            productService.searchProducts("xyzzy_nonexistent_product_12345");
        });
    }

    // -----------------------------------------------------------------------
    // deleteProduct tests
    // -----------------------------------------------------------------------

    @Test
    void deleteProduct_withValidId_returnsSuccess() throws SQLException {
        // Create a product to delete
        long productToDelete = TestUtils.insertTestProduct(
                "To Be Deleted", "DEL-" + System.currentTimeMillis(),
                10, 5, categoryId);

        Response response = productService.deleteProduct(productToDelete);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Product Deleted successfully", response.getMessage());
    }

    @Test
    void deleteProduct_withInvalidId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> {
            productService.deleteProduct(999999999L);
        });
    }
}
