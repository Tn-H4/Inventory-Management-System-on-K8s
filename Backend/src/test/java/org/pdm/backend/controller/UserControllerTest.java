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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController (/api/users/**).
 *
 * NOTE: @Transactional/@Rollback have no effect — see AuthControllerTest for explanation.
 * Cleanup is done manually in @AfterEach.
 *
 * NOTE: STAFF role does not exist in UserRole.java (only ADMIN and MANAGER).
 * Tests that require a STAFF token are documented but cannot run until STAFF is added.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL   = "test.user.admin@test.local";
    private static final String MANAGER_EMAIL = "test.user.manager@test.local";
    private static final String TARGET_EMAIL  = "test.user.target@test.local";

    private long adminId;
    private long managerId;
    private long targetId;

    private String adminToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
        TestUtils.deleteUserByEmail(TARGET_EMAIL);

        adminId   = TestUtils.insertTestUser("Admin",   ADMIN_EMAIL,   ENCODER.encode("admin123"),   "ADMIN");
        managerId = TestUtils.insertTestUser("Manager", MANAGER_EMAIL, ENCODER.encode("manager123"), "MANAGER");
        targetId  = TestUtils.insertTestUser("Target",  TARGET_EMAIL,  ENCODER.encode("target123"),  "MANAGER");

        adminToken   = TestUtils.loginAndGetToken(mockMvc, ADMIN_EMAIL,   "admin123");
        managerToken = TestUtils.loginAndGetToken(mockMvc, MANAGER_EMAIL, "manager123");
    }

    @AfterEach
    void tearDown() throws Exception {
        // targetId may have been deleted in a test; deleteUserByEmail is a no-op if not found
        TestUtils.deleteUserByEmail(ADMIN_EMAIL);
        TestUtils.deleteUserByEmail(MANAGER_EMAIL);
        TestUtils.deleteUserByEmail(TARGET_EMAIL);
    }

    // -----------------------------------------------------------------------
    // GET /api/users/all
    // -----------------------------------------------------------------------

    @Test
    void getAllUsers_withAdminToken_returns200AndUsersArray() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.users").isArray());
    }

    @Test
    void getAllUsers_withManagerToken_returns403WithJsonBody() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAllUsers_withNoToken_returns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // BUG: STAFF role does not exist in UserRole enum.
    // Once UserRole.STAFF is added, uncomment and enable this test:
    //
    // @Test
    // void getAllUsers_withStaffToken_returns403() throws Exception {
    //     long staffId = TestUtils.insertTestUser("Staff", "test.user.staff@test.local",
    //                                             ENCODER.encode("staff123"), "STAFF");
    //     String staffToken = TestUtils.loginAndGetToken(mockMvc, "test.user.staff@test.local", "staff123");
    //     mockMvc.perform(get("/api/users/all").header("Authorization", "Bearer " + staffToken))
    //             .andExpect(status().isForbidden());
    //     TestUtils.deleteUserById(staffId);
    // }

    // -----------------------------------------------------------------------
    // GET /api/users/current
    // -----------------------------------------------------------------------

    @Test
    void getCurrentUser_withValidToken_returns200AndCorrectEmail() throws Exception {
        mockMvc.perform(get("/api/users/current")
                        .header("Authorization", "Bearer " + adminToken))
                // NOTE: this endpoint returns User directly (not wrapped in Response envelope).
                // See UserController.getCurrentUser() — returns ResponseEntity<User>.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                // BUG: UserServiceImpl.getCurrentLoggedInUser() returns the raw User from the DB,
                // which still has the BCrypt password hash set. Should null it before returning.
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getCurrentUser_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/current"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // GET /api/users/{id}
    // -----------------------------------------------------------------------

    @Test
    void getUserById_withValidToken_returns200AndUser() throws Exception {
        mockMvc.perform(get("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.user.id").value(adminId));
    }

    @Test
    void getUserById_forNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/users/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/users/delete/{id}
    // -----------------------------------------------------------------------

    @Test
    void deleteUser_withAdminToken_returns200() throws Exception {
        mockMvc.perform(delete("/api/users/delete/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").exists());
        // targetId already deleted — @AfterEach deleteUserByEmail is a no-op for TARGET_EMAIL
    }

    @Test
    void deleteUser_withManagerToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/delete/" + targetId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteUser_withNoToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/delete/" + targetId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_forNonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/users/delete/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
