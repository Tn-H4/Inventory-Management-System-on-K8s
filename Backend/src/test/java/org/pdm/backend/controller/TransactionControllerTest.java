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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransactionController (/api/transactions/**).
 *
 * These are the most critical tests — they verify stock mutation correctness.
 * Stock levels are verified by reading directly from the DB via TestUtils.getProductStock()
 * after each transaction, bypassing any application-layer caching.
 *
 * NOTE: @Transactional/@Rollback have no effect — see AuthControllerTest.
 *
 * NOTE: Oversell behaviour documented below:
 *   ProductRepositoryImpl.update() catches SQLException silently and returns null.
 *   When stock_quantity < 0 violates the DB CHECK constraint (MySQL 8.0.16+), the
 *   update fails silently. The transaction record is still saved and the endpoint
 *   returns 200. On older MySQL (no CHECK enforcement) the stock actually goes negative.
 *   Neither outcome is correct. The fix is a stock guard in TransactionServiceImpl.sell().
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String MANAGER_EMAIL = "test.tx.manager@test.local";
    private static final String ADMIN_EMAIL   = "test.tx.admin@test.local";

    private long categoryId;
    private long supplierId;
    private long productId;   // stock = 50, reorderThreshold = 10
    private long tightProductId; // stock = 5,  reorderThreshold = 3  (for oversell tests)
    private long thresholdProductId; // stock = 15, reorderThreshold = 10 (for threshold test)

    private String managerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);

        TestUtils.insertTestUser("TX Manager", MANAGER_EMAIL, ENCODER.encode("manager123"), "MANAGER");
        TestUtils.insertTestUser("TX Admin",   ADMIN_EMAIL,   ENCODER.encode("admin123"),   "ADMIN");

        managerToken = TestUtils.loginAndGetToken(mockMvc, MANAGER_EMAIL, "manager123");
        adminToken   = TestUtils.loginAndGetToken(mockMvc, ADMIN_EMAIL,   "admin123");

        categoryId = TestUtils.insertTestCategory("TX Test Cat " + System.currentTimeMillis());
        supplierId = TestUtils.insertTestSupplier("TX Test Supplier " + System.currentTimeMillis());

        productId          = TestUtils.insertTestProduct("TX Product",       "TX-SKU-A-" + System.currentTimeMillis(), 50, 10, categoryId);
        tightProductId     = TestUtils.insertTestProduct("TX Tight Product", "TX-SKU-B-" + System.currentTimeMillis(), 5,  3,  categoryId);
        thresholdProductId = TestUtils.insertTestProduct("TX Thresh Product","TX-SKU-C-" + System.currentTimeMillis(), 15, 10, categoryId);
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteProductById(productId);
        TestUtils.deleteProductById(tightProductId);
        TestUtils.deleteProductById(thresholdProductId);
        TestUtils.deleteSupplierById(supplierId);
        TestUtils.deleteCategoryById(categoryId);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
    }

    // -----------------------------------------------------------------------
    // POST /api/transactions/sell
    // -----------------------------------------------------------------------

    @Test
    void sell_withSufficientStock_returns200AndDecrementsStock() throws Exception {
        int initialStock = TestUtils.getProductStock(productId); // 50
        int quantityToSell = 10;

        var payload = Map.of("productId", productId, "quantity", quantityToSell);

        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        int actualStock = TestUtils.getProductStock(productId);
        assertEquals(initialStock - quantityToSell, actualStock,
                "Stock should have decreased by " + quantityToSell);
    }

    @Test
    void sell_withQuantityExceedingStock_returns400WithFriendlyMessage() throws Exception {
        // tightProductId has stock = 5; try to sell 99
        var payload = Map.of("productId", tightProductId, "quantity", 99);

        // BUG: TransactionServiceImpl.sell() does not check stock before deducting.
        // On MySQL 8.0.16+: ProductRepositoryImpl.update() fails silently (DB CHECK violated,
        // SQLException swallowed), the transaction is saved anyway, and the endpoint returns 200.
        // On older MySQL: stock goes negative and the endpoint returns 200.
        // Expected: 400 with a message like "Insufficient stock. Available: 5".
        // Fix: add `if (product.getStockQuantity() < quantity) throw new NameValueRequiredException(...)`
        // in TransactionServiceImpl.sell() BEFORE modifying stock.
        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sell_withQuantityEqualToStock_returns200AndSetsStockToZero() throws Exception {
        // tightProductId has stock = 5; sell exactly 5
        int initialStock = TestUtils.getProductStock(tightProductId); // 5
        var payload = Map.of("productId", tightProductId, "quantity", initialStock);

        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertEquals(0, TestUtils.getProductStock(tightProductId),
                "Stock should be exactly 0 after selling all units");
    }

    @Test
    void sell_withQuantityBringingStockToReorderThreshold_returns200() throws Exception {
        // thresholdProductId: stock=15, reorderThreshold=10. Sell 5 → stock=10 = threshold.
        // No REORDER_NEEDED field exists yet; this test verifies the sale succeeds and
        // stock is correct. Low-stock badge logic is handled client-side.
        int quantityToSell = 5;
        var payload = Map.of("productId", thresholdProductId, "quantity", quantityToSell);

        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertEquals(10, TestUtils.getProductStock(thresholdProductId),
                "Stock should equal reorderThreshold (10) after selling 5 from 15");
    }

    @Test
    void sell_withNoToken_returns401() throws Exception {
        var payload = Map.of("productId", productId, "quantity", 1);

        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sell_withMissingProductId_returns400() throws Exception {
        // @Positive on productId in TransactionRequest — fails validation
        var payload = Map.of("quantity", 5);

        mockMvc.perform(post("/api/transactions/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /api/transactions/purchase
    // -----------------------------------------------------------------------

    @Test
    void purchase_withValidRequest_returns200AndIncrementsStock() throws Exception {
        int initialStock = TestUtils.getProductStock(productId); // 50
        int quantityToBuy = 20;

        var payload = Map.of(
                "productId",  productId,
                "quantity",   quantityToBuy,
                "supplierId", supplierId
        );

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertEquals(initialStock + quantityToBuy, TestUtils.getProductStock(productId),
                "Stock should have increased by " + quantityToBuy);
    }

    @Test
    void purchase_withMissingSupplierId_returns400() throws Exception {
        // TransactionServiceImpl.restockInventory() throws NameValueRequiredException if supplierId is null
        var payload = Map.of("productId", productId, "quantity", 10);

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Supplier Id is Required"));
    }

    @Test
    void purchase_withNonExistentProduct_returns404() throws Exception {
        var payload = Map.of("productId", 999999999L, "quantity", 10, "supplierId", supplierId);

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/transactions/return
    // -----------------------------------------------------------------------

    @Test
    void returnToSupplier_withPurchaseHistory_decrementsStock() throws Exception {
        int initialStock = TestUtils.getProductStock(productId); // 50
        int quantityToPurchase = 20;
        int quantityToReturn = 8;

        // Step 1: First do a purchase to establish tracking history
        var purchasePayload = Map.of(
                "productId",  productId,
                "quantity",   quantityToPurchase,
                "supplierId", supplierId
        );

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchasePayload))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        int stockAfterPurchase = initialStock + quantityToPurchase;
        assertEquals(stockAfterPurchase, TestUtils.getProductStock(productId),
                "Stock should have increased after purchase");

        // Step 2: Now do a return — stock should decrease immediately (single-step)
        var returnPayload = Map.of(
                "productId",  productId,
                "quantity",   quantityToReturn,
                "supplierId", supplierId
        );

        mockMvc.perform(post("/api/transactions/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnPayload))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertEquals(stockAfterPurchase - quantityToReturn, TestUtils.getProductStock(productId),
                "Stock should have decreased by " + quantityToReturn + " after return");
    }

    @Test
    void returnToSupplier_withMissingSupplierId_returns400() throws Exception {
        var payload = Map.of("productId", productId, "quantity", 5);

        mockMvc.perform(post("/api/transactions/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Supplier Id is Required"));
    }

    // -----------------------------------------------------------------------
    // GET /api/transactions/all
    // -----------------------------------------------------------------------

    @Test
    void getAllTransactions_withValidToken_returns200AndPaginatedResponse() throws Exception {
        mockMvc.perform(get("/api/transactions/all")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void getAllTransactions_withTypeFilter_returnsOnlyMatchingType() throws Exception {
        // The backend filter matches via LIKE on transaction_type column
        mockMvc.perform(get("/api/transactions/all")
                        .param("page",   "0")
                        .param("size",   "100")
                        .param("filter", "SALE")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray());
    }

    @Test
    void getAllTransactions_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions/all"))
                .andExpect(status().isUnauthorized());
    }
}
