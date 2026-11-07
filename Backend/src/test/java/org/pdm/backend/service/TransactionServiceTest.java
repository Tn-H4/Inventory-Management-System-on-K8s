package org.pdm.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdm.backend.exception.NameValueRequiredException;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.Product;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.ProductRepository;
import org.pdm.backend.repository.SupplierRepository;
import org.pdm.backend.security.AuthUser;
import org.pdm.backend.service.Impl.TransactionServiceImpl;
import org.pdm.backend.util.TestUtils;
import org.pdm.backend.wrappers.Response;
import org.pdm.backend.wrappers.TransactionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionServiceImpl.
 * Tests purchase, sale, return, and query operations for transactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionServiceImpl transactionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private long categoryId;
    private long productId;
    private long supplierId;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        // Create a User object and wrap it in AuthUser
        // Use the email from the existing test database user (id=1)
        User user = User.builder()
                .id(1L)
                .email("admin@gmail.com")
                .name("admin user")
                .password("$2a$10$eUSKrqVAFm/rThmuBv858Oc/5pemWwpnm3PufrkaFUj.YLV6xxhtq")
                .role(org.pdm.backend.enums.UserRole.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        // Set up security context with a mock admin user
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var auth = new UsernamePasswordAuthenticationToken(
                AuthUser.builder().user(user).build(),
                null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        categoryId = TestUtils.insertTestCategory("Transaction Test Category " + System.currentTimeMillis());
        productId = TestUtils.insertTestProduct(
                "Transaction Test Product", "TXN-TST-" + System.currentTimeMillis(),
                100, 20, categoryId);
        supplierId = TestUtils.insertTestSupplier("Transaction Test Supplier " + System.currentTimeMillis());
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteProductById(productId);
        TestUtils.deleteCategoryById(categoryId);
        TestUtils.deleteSupplierById(supplierId);
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // restockInventory (Purchase) tests
    // -----------------------------------------------------------------------

    @Test
    void restockInventory_withValidData_returnsSuccess() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setSupplierId(supplierId);
        request.setQuantity(50);
        request.setDescription("Test restock");

        Response response = transactionService.restockInventory(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Transaction Made Successfully", response.getMessage());
    }

    @Test
    void restockInventory_withInvalidProduct_throwsNotFoundException() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(999999999L);
        request.setSupplierId(supplierId);
        request.setQuantity(10);

        assertThrows(NotFoundException.class, () -> {
            transactionService.restockInventory(request);
        });
    }

    @Test
    void restockInventory_withInvalidSupplier_throwsNotFoundException() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setSupplierId(999999999L);
        request.setQuantity(10);

        assertThrows(NotFoundException.class, () -> {
            transactionService.restockInventory(request);
        });
    }

    @Test
    void restockInventory_withMissingSupplier_throwsNameValueRequiredException() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setSupplierId(null); // Missing supplier
        request.setQuantity(10);

        assertThrows(NameValueRequiredException.class, () -> {
            transactionService.restockInventory(request);
        });
    }

    // -----------------------------------------------------------------------
    // sell tests
    // -----------------------------------------------------------------------

    @Test
    void sell_withValidData_returnsSuccess() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setQuantity(10);
        request.setDescription("Test sale");

        Response response = transactionService.sell(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Transaction Sold Successfully", response.getMessage());
    }

    @Test
    void sell_withInvalidProduct_throwsNotFoundException() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(999999999L);
        request.setQuantity(10);

        assertThrows(NotFoundException.class, () -> {
            transactionService.sell(request);
        });
    }

    // -----------------------------------------------------------------------
    // returnToSupplier tests
    // -----------------------------------------------------------------------

    @Test
    void returnToSupplier_withValidData_returnsSuccess() {
        // First do a purchase to establish tracking history
        TransactionRequest purchaseRequest = new TransactionRequest();
        purchaseRequest.setProductId(productId);
        purchaseRequest.setSupplierId(supplierId);
        purchaseRequest.setQuantity(10);
        transactionService.restockInventory(purchaseRequest);

        // Now do the return
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setSupplierId(supplierId);
        request.setQuantity(5);
        request.setDescription("Test return");

        Response response = transactionService.returnToSupplier(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Transaction Returned Successfully Initialized", response.getMessage());
    }

    @Test
    void returnToSupplier_withMissingSupplier_throwsNameValueRequiredException() {
        TransactionRequest request = new TransactionRequest();
        request.setProductId(productId);
        request.setSupplierId(null);
        request.setQuantity(5);

        assertThrows(NameValueRequiredException.class, () -> {
            transactionService.returnToSupplier(request);
        });
    }

    // -----------------------------------------------------------------------
    // getAllTransactions tests
    // -----------------------------------------------------------------------

    @Test
    void getAllTransactions_withNoFilters_returnsTransactionsWithPagination() {
        Response response = transactionService.getAllTransactions(0, 20, null, null, "created_at", "DESC");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getTransactions());
        assertNotNull(response.getTotalElements());
        assertNotNull(response.getTotalPages());
    }

    @Test
    void getAllTransactions_withFilter_returnsFilteredTransactions() {
        Response response = transactionService.getAllTransactions(0, 20, "PURCHASE", null, "created_at", "DESC");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getTransactions());
    }

    @Test
    void getAllTransactions_withSortByPrice_returnsSortedResults() {
        Response response = transactionService.getAllTransactions(0, 20, null, null, "total_price", "DESC");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getTransactions());
    }

    // -----------------------------------------------------------------------
    // getTransactionById tests
    // -----------------------------------------------------------------------

    @Test
    void getTransactionById_withInvalidId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> {
            transactionService.getTransactionById(999999999L);
        });
    }

    // -----------------------------------------------------------------------
    // getAllTransactionByMonthAndYear tests
    // -----------------------------------------------------------------------

    @Test
    void getAllTransactionByMonthAndYear_returnsTransactionsForMonth() {
        Response response = transactionService.getAllTransactionByMonthAndYear(1, 2025);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getTransactions());
    }
}
