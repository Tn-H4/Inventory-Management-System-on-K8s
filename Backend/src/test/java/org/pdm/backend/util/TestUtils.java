package org.pdm.backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdm.backend.security.DatabaseConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared JDBC helpers and MockMvc login helper for integration tests.
 *
 * IMPORTANT: All JDBC methods here call DatabaseConfig.getConnection(), which
 * is a static DriverManager call hardcoded to the production DB credentials.
 * There is no Spring transaction involved — changes are committed immediately
 * and must be undone in @AfterEach.
 */
public final class TestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestUtils() {}

    // -----------------------------------------------------------------------
    // Auth helpers
    // -----------------------------------------------------------------------

    /**
     * Performs POST /api/auth/login via MockMvc and returns the JWT token.
     * Returns an empty string if login fails (callers should assert non-empty).
     */
    public static String loginAndGetToken(MockMvc mockMvc, String email, String password) throws Exception {
        String body = MAPPER.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return MAPPER.readTree(json).path("token").asText("");
    }

    // -----------------------------------------------------------------------
    // User helpers
    // -----------------------------------------------------------------------

    /**
     * Inserts a user directly via JDBC and returns the generated ID.
     *
     * @param hashedPassword must already be BCrypt-hashed by the caller
     * @param role           "ADMIN" or "MANAGER"
     */
    public static long insertTestUser(String name, String email,
                                      String hashedPassword, String role) throws SQLException {
        String sql = """
                INSERT INTO users (name, email, password, phone_number, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.setString(4, "0900000000");
            ps.setString(5, role);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("User insert returned no generated key");
    }

    public static void deleteUserByEmail(String email) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    public static void deleteUserById(long id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Category helpers
    // -----------------------------------------------------------------------

    public static long insertTestCategory(String name) throws SQLException {
        String sql = """
                INSERT INTO categories (name, description, image_url, is_active, display_order, created_at)
                VALUES (?, 'Test category', NULL, TRUE, 0, NOW())
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Category insert returned no generated key");
    }

    public static void deleteCategoryById(long id) throws SQLException {
        // products referencing this category must be removed first
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Product helpers
    // -----------------------------------------------------------------------

    /**
     * Inserts a product and returns its generated ID.
     *
     * @param stockQuantity   starting stock level
     * @param reorderThreshold low-stock alert threshold
     */
    public static long insertTestProduct(String name, String sku,
                                         int stockQuantity, int reorderThreshold,
                                         long categoryId) throws SQLException {
        String sql = """
                INSERT INTO products
                    (name, sku, price, stock_quantity, reorder_threshold,
                     description, expiry_date, image_url, category_id, created_at)
                VALUES (?, ?, ?, ?, ?, 'Test product', ?, '/test/img.jpg', ?, NOW())
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, sku);
            ps.setBigDecimal(3, new BigDecimal("100000"));
            ps.setInt(4, stockQuantity);
            ps.setInt(5, reorderThreshold);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now().plusYears(2)));
            ps.setLong(7, categoryId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Product insert returned no generated key");
    }

    /**
     * Deletes a product and all FK-dependent rows (transactions, product_suppliers).
     */
    public static void deleteProductById(long id) throws SQLException {
        for (String sql : new String[]{
                "DELETE FROM transactions WHERE product_id = ?",
                "DELETE FROM product_suppliers WHERE product_id = ?",
                "DELETE FROM products WHERE id = ?"
        }) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    /** Reads the current stock_quantity from the DB (bypasses any caching). */
    public static int getProductStock(long productId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT stock_quantity FROM products WHERE id = ?")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Product not found: " + productId);
    }

    // -----------------------------------------------------------------------
    // Supplier helpers
    // -----------------------------------------------------------------------

    public static long insertTestSupplier(String name) throws SQLException {
        String sql = """
                INSERT INTO suppliers (name, address, contact_info, email, website, tax_code, status, created_at)
                VALUES (?, 'Test Address', '+84 900 000 000', ?, 'https://test.com', '0000000000', 'ACTIVE', NOW())
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, name.toLowerCase().replace(" ", "") + "@test.local");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Supplier insert returned no generated key");
    }

    public static void deleteSupplierById(long id) throws SQLException {
        for (String sql : new String[]{
                "DELETE FROM product_suppliers WHERE supplier_id = ?",
                "DELETE FROM transactions WHERE supplier_id = ?",
                "DELETE FROM suppliers WHERE id = ?"
        }) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Transaction helpers
    // -----------------------------------------------------------------------

    public static void deleteTransactionsByProductId(long productId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM transactions WHERE product_id = ?")) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        }
    }

    /** Returns the ID of the most recently created transaction for a given product. */
    public static long getLatestTransactionId(long productId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM transactions WHERE product_id = ? ORDER BY created_at DESC LIMIT 1")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("No transaction found for product: " + productId);
    }
}
