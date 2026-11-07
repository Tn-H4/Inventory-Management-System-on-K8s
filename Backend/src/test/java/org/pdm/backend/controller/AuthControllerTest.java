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

import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController (POST /api/auth/login).
 *
 * NOTE: @Transactional and @Rollback are declared to satisfy the requirement but have
 * NO EFFECT here. All repositories call DatabaseConfig.getConnection() which uses raw
 * DriverManager — completely outside Spring's transaction management. Cleanup is
 * performed manually in @AfterEach via direct JDBC deletes.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    // Stable test emails — unique to this class so they don't collide with seed data
    private static final String ADMIN_EMAIL    = "test.auth.admin@test.local";
    @BeforeEach
    void setUp() throws Exception {
        // Clean up any residue from a previous interrupted run
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);

        // Seed one ADMIN user for login tests
        TestUtils.insertTestUser("Auth Test Admin", ADMIN_EMAIL, ENCODER.encode("admin123"), "ADMIN");
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
    }

    // -----------------------------------------------------------------------

    @Test
    void login_withCorrectCredentials_returns200WithTokenAndRole() throws Exception {
        var payload = Map.of("email", ADMIN_EMAIL, "password", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                // BUG: user.password is the BCrypt hash — should be null in login response.
                // Fix: add `user.setPassword(null);` before building the Response in loginUser().
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void login_withWrongPassword_returnsClientError() throws Exception {
        var payload = Map.of("email", ADMIN_EMAIL, "password", "COMPLETELY_WRONG");

        // BUG: UserServiceImpl.loginUser() throws InvalidCredentialsException("Password Does Not Match").
        // GlobalExceptionHandler maps InvalidCredentialsException to HttpStatus.BAD_REQUEST (400),
        // not UNAUTHORIZED (401). Fix: change the handler to return 401 for this exception type.
        // This test asserts the EXPECTED behaviour (401); currently returns 400.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_withNonExistentEmail_returnsClientError() throws Exception {
        var payload = Map.of("email", "nobody@test.local", "password", "password123");

        // BUG: UserServiceImpl.loginUser() throws NotFoundException("Email Not Found").
        // GlobalExceptionHandler maps NotFoundException to 404, not 401.
        // Fix: throw InvalidCredentialsException (or a new UnauthorizedException mapped to 401)
        // for "email not found" to prevent email enumeration and return the correct HTTP code.
        // This test asserts the EXPECTED behaviour (401); currently returns 404.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }
}
