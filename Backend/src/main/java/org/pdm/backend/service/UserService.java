package org.pdm.backend.service;


import org.pdm.backend.model.User;
import org.pdm.backend.wrappers.AdminCreateUserRequest;
import org.pdm.backend.wrappers.LoginRequest;
import org.pdm.backend.wrappers.RegisterRequest;
import org.pdm.backend.wrappers.Response;

public interface UserService {
    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest The login request containing username and password.
     * @return A Response containing the authentication token and user details.
     */
    Response loginUser(LoginRequest loginRequest);

    /**
     * Retrieves a paginated and filtered list of all users.
     *
     * @param search Search query for username, email, or full name.
     * @param role Filter by user role.
     * @param status Filter by user status.
     * @param startDate Filter users created from this date.
     * @param endDate Filter users created until this date.
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @param page The page number to retrieve.
     * @param limit The number of records per page.
     * @return A Response containing the paginated list of users.
     */
    Response getAllUsers(String search, String role, String status, String startDate, String endDate, String sortBy, String sortDir, int page, int limit);

    /**
     * Retrieves the details of the currently authenticated user.
     *
     * @return The User object of the currently logged-in user.
     */
    User getCurrentLoggedInUser();

    /**
     * Retrieves a specific user by their ID.
     *
     * @param id The ID of the user.
     * @return A Response containing the user details.
     * @throws org.pdm.backend.exception.NotFoundException if the user is not found.
     */
    Response getUserById(Long id);

    /**
     * Updates an existing user's profile details.
     *
     * @param id The ID of the user to update.
     * @param user The updated user details.
     * @return A Response indicating the result of the update operation.
     * @throws org.pdm.backend.exception.NotFoundException if the user is not found.
     */
    Response updateUser(Long id, User user);

    /**
     * Deletes a user by their ID (soft delete).
     *
     * @param id The ID of the user to delete.
     * @return A Response indicating the result of the deletion.
     * @throws org.pdm.backend.exception.NotFoundException if the user is not found.
     */
    Response deleteUser(Long id);

    /**
     * Reactivates a previously deleted/deactivated user.
     *
     * @param id The ID of the user to reactivate.
     * @return A Response indicating the result of the reactivation.
     * @throws org.pdm.backend.exception.NotFoundException if the user is not found.
     */
    Response reactivateUser(Long id);

    /**
     * Retrieves all transactions associated with a specific user.
     *
     * @param id The ID of the user.
     * @return A Response containing the list of transactions.
     */
    Response getUserTransactions(Long id);

    /**
     * Changes the password for a specific user.
     *
     * @param id The ID of the user.
     * @param currentPassword The current password for verification.
     * @param newPassword The new password to be set.
     * @return A Response indicating the result of the password change.
     * @throws org.pdm.backend.exception.NotFoundException if the user is not found.
     */
    Response changePassword(Long id, String currentPassword, String newPassword);

    /**
     * Registers a new user in the system.
     *
     * @param request The registration request containing user details.
     * @return A Response indicating the result of the registration.
     */
    Response registerUser(RegisterRequest request);

    /**
     * Creates a new user via an administrative request.
     *
     * @param request The admin create user request.
     * @return A Response indicating the result of the user creation.
     */
    Response createUser(AdminCreateUserRequest request);

    /**
     * Exports all users to a CSV format.
     *
     * @return A CSV string containing the user data.
     */
    String exportUsersCsv();
}
