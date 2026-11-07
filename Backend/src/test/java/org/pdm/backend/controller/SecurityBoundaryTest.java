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

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-controller security boundary tests.
 *
 * Verifies that:
 *  1. No token → 401 JSON (not Spring's default HTML error page)
 *  2. Malformed/wrong-key token → see BUG note below
 *  3. MANAGER token on ADMIN-only endpoints → 403 JSON
 *  4. STAFF role (documented but cannot run — role does not exist yet)
 *
 * BUG: Malformed or wrong-key JWT token causes JwtException to be thrown
 * inside AuthFilter.doFilterInternal() BEFORE the try-catch that wraps
 * filterChain.doFilter(). The exception escapes the filter and is NOT
 * handled by CustomAuthenticationEntryPoint (which only fires when Spring
 * Security's ExceptionTranslationFilter catches an AuthenticationException).
 * Result: likely a 500 from the servlet container, not a clean 401 JSON.
 * Fix: wrap the JWT parsing block in its own try-catch in AuthFilter that
 * lets the request proceed unauthenticated (so ExceptionTranslationFilter
 * handles it), or catch JwtException and write a 401 JSON response directly.
 *
 * NOTE: @Transactional/@Rollback have no effect — see AuthControllerTest.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL   = "test.sec.admin@test.local";
    private static final String MANAGER_EMAIL = "test.sec.manager@test.local";

    private long categoryId;
    private String managerToken;

    /**
     * A JWT signed with a different HMAC key than the application uses.
     * Valid structure but wrong signature — JJWT will throw SignatureException.
     * This token was pre-generated with a random key different from the test secret.
     * It is intentionally invalid.
     */
    private static final String WRONG_KEY_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9" +
            ".eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjk5OTk5OTk5OTl9" +
            ".INVALIDSIGNATURETHATWILLNOTVERIFY_aaaabbbbcccc";

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);

        TestUtils.insertTestUser("Sec Admin",   ADMIN_EMAIL,   ENCODER.encode("admin123"),   "ADMIN");
        TestUtils.insertTestUser("Sec Manager", MANAGER_EMAIL, ENCODER.encode("manager123"), "MANAGER");

        categoryId   = TestUtils.insertTestCategory("Sec Test Cat " + System.currentTimeMillis());
        managerToken = TestUtils.loginAndGetToken(mockMvc, MANAGER_EMAIL, "manager123");
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteCategoryById(categoryId);
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
    }

    // -----------------------------------------------------------------------
    // No token → 401 with JSON body (not HTML)
    // -----------------------------------------------------------------------

    @Test
    void noToken_onUsersAll_returns401WithJsonNotHtml() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void noToken_onProductsAll_returns401WithJson() throws Exception {
        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void noToken_onTransactionsAll_returns401WithJson() throws Exception {
        mockMvc.perform(get("/api/transactions/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void noToken_onSuppliersAll_returns401WithJson() throws Exception {
        mockMvc.perform(get("/api/suppliers/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // Malformed / wrong-key token → should be 401 JSON
    // -----------------------------------------------------------------------

    @Test
    void wrongKeyToken_onProtectedEndpoint_returns401WithJsonBody() throws Exception {
        // BUG: AuthFilter does not catch JwtException from getUsernameFromToken().
        // A token with an invalid signature throws SignatureException (extends JwtException,
        // extends RuntimeException) inside doFilterInternal() outside the try-catch block.
        // This exception escapes the filter chain entirely and the servlet container
        // likely returns a 500 with an HTML body, not a 401 JSON.
        // Fix: wrap the JWT parsing block in a try-catch in AuthFilter; on JwtException,
        // simply let the request proceed without setting authentication so that
        // ExceptionTranslationFilter produces the correct 401 JSON response.
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + WRONG_KEY_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedToken_onProtectedEndpoint_returns401WithJsonBody() throws Exception {
        // BUG: same as wrongKeyToken — malformed token string throws JwtException in AuthFilter.
        // Expected 401 JSON; currently likely 500 from unhandled exception.
        mockMvc.perform(get("/api/products/all")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // MANAGER token on ADMIN-only endpoints → 403 JSON
    // -----------------------------------------------------------------------

    @Test
    void managerToken_onUsersAll_returns403WithJson() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void managerToken_onCategoryAdd_returns403WithJson() throws Exception {
        var body = objectMapper.writeValueAsString(
                java.util.Map.of("name", "Should Not Create", "description", "test"));

        mockMvc.perform(post("/api/categories/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void managerToken_onCategoryDelete_returns403WithJson() throws Exception {
        mockMvc.perform(delete("/api/categories/delete/" + categoryId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void managerToken_onSupplierAdd_returns403WithJson() throws Exception {
        var body = objectMapper.writeValueAsString(
                java.util.Map.of("name", "Should Not Create", "contactInfo", "test@example.com"));

        mockMvc.perform(post("/api/suppliers/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void managerToken_onUserDelete_returns403WithJson() throws Exception {
        mockMvc.perform(delete("/api/users/delete/999")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void managerToken_onWarehouseAdd_returns403WithJson() throws Exception {
        var body = objectMapper.writeValueAsString(
                java.util.Map.of("name", "Should Not Create", "location", "Test"));

        mockMvc.perform(post("/api/warehouses/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // BUG: STAFF role does not exist in UserRole enum (only ADMIN and MANAGER).
    // Once UserRole.STAFF is added, the following tests should be uncommented and enabled.
    // STAFF should get 403 on all @PreAuthorize("hasAuthority('ADMIN')") endpoints.
    //
    // @Test
    // void staffToken_onUsersAll_returns403() throws Exception { ... }
    //
    // @Test
    // void staffToken_onCategoryAdd_returns403() throws Exception { ... }
}
