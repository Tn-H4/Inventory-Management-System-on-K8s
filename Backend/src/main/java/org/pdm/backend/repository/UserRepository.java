package org.pdm.backend.repository;

import org.pdm.backend.enums.UserRole;
import org.pdm.backend.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository {
    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user.
     * @return An Optional containing the user if found.
     */
    Optional<User> findById(Long id);
    /**
     * Retrieves a user by their email.
     *
     * @param email The email of the user.
     * @return An Optional containing the user if found.
     */
    Optional<User> findByEmail(String email);
    /**
     * Retrieves all users with dynamic sorting.
     *
     * @param sortBy The field to sort by.
     * @param sortDir The direction of sorting (ASC or DESC).
     * @return A list of all users.
     */
    List<User> findAll(String sortBy, String sortDir);
    /**
     * Retrieves a paginated list of users based on search and filter criteria.
     *
     * @param search    The search keyword for username or email.
     * @param role      The user role filter.
     * @param status    The account status filter.
     * @param startDate The starting date for account creation filter.
     * @param endDate   The ending date for account creation filter.
     * @param sortBy    The field to sort by.
     * @param sortDir   The direction of sorting (ASC or DESC).
     * @param page      The page number (0-indexed).
     * @param size      The number of elements per page.
     * @return A list of users for the specified page.
     */
    List<User> findAllPaged(String search, String role, String status, String startDate, String endDate, String sortBy, String sortDir, int page, int size);
    /**
     * Counts the total number of users matching the given search and filter criteria.
     *
     * @param search    The search keyword for username or email.
     * @param role      The user role filter.
     * @param status    The account status filter.
     * @param startDate The starting date for account creation filter.
     * @param endDate   The ending date for account creation filter.
     * @return The total number of matching users.
     */
    long countAll(String search, String role, String status, String startDate, String endDate);
    /**
     * Retrieves all users belonging to a specific role.
     *
     * @param role The UserRole to filter by.
     * @return A list of users with the specified role.
     */
    List<User> findByRole(UserRole role);
    /**
     * Deletes a user by their ID (soft delete).
     *
     * @param id The ID of the user to delete.
     * @return The number of affected rows.
     */
    Long deleteById(Long id);
    /**
     * Reactivates a user by their ID.
     *
     * @param id The ID of the user to reactivate.
     * @return The number of affected rows.
     */
    Long activateById(Long id);
    /**
     * Retrieves all active users.
     *
     * @return A list of active users.
     */
    List<User> findAllActive();
    /**
     * Saves a new user.
     *
     * @param user The user to save.
     * @return The saved User object.
     */
    User save(User user);
    /**
     * Updates an existing user.
     *
     * @param user The user to update.
     * @return The updated User object.
     */
    User update(User user);
    /**
     * Checks if a user with the given email already exists.
     *
     * @param email The email to check.
     * @return True if the email is already registered.
     */
    boolean existsByEmail(String email);
}


