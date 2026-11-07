package org.pdm.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import org.pdm.backend.enums.UserRole;
import org.pdm.backend.exception.InvalidCredentialsException;
import org.pdm.backend.exception.NameValueRequiredException;
import org.pdm.backend.exception.NotFoundException;
import org.pdm.backend.model.User;
import org.pdm.backend.repository.UserRepository;
import org.pdm.backend.security.JwtUtils;
import org.pdm.backend.service.UserService;
import org.pdm.backend.wrappers.AdminCreateUserRequest;
import org.pdm.backend.wrappers.LoginRequest;
import org.pdm.backend.wrappers.RegisterRequest;
import org.pdm.backend.wrappers.Response;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.pdm.backend.util.CsvExportUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Sanitizes user object by nullifying password before returning in API responses.
     */
    private User sanitizeUser(User user) {
        user.setPassword(null);
        return user;
    }

    /**
     * Shared logic for creating and persisting a new user with a given role.
     * Throws NameValueRequiredException if the email is already registered.
     */
    private User buildAndSaveUser(String name, String email, String password, String phoneNumber, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new NameValueRequiredException("Email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return sanitizeUser(user);
    }

    /**
     * Authorization check: users can only change their own password unless they are ADMIN.
     */
    private void authorizePasswordChange(Long targetUserId) {
        User caller = getCurrentLoggedInUser();
        if (!caller.getId().equals(targetUserId) && caller.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You can only change your own password");
        }
    }

    /**
     * Authenticates a user using email and password and returns a JWT token.
     *
     * @param loginRequest The login request containing email and password.
     * @return A Response containing the authentication token, user role, and sanitized user details.
     * @throws InvalidCredentialsException if credentials are invalid or account is deactivated.
     */
    @Override
    public Response loginUser(LoginRequest loginRequest) {

        // B3: Throw InvalidCredentialsException instead of NotFoundException for non-existent email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is deactivated. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getEmail());

        return Response.builder()
                .status(200)
                .message("User Logged in Successfully")
                .role(user.getRole())
                .token(token)
                .expirationTime("6 months")
                .user(sanitizeUser(user))
                .build();
    }

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
     * @return A Response containing the paginated list of sanitized users.
     */
    @Override
    public Response getAllUsers(String search, String role, String status, String startDate, String endDate, String sortBy, String sortDir, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        List<User> users = userRepository.findAllPaged(search, role, status, startDate, endDate, sortBy, sortDir, safePage, safeLimit);
        users.forEach(this::sanitizeUser);

        long total = userRepository.countAll(search, role, status, startDate, endDate);
        int totalPages = (int) Math.ceil((double) total / safeLimit);

        return Response.builder()
                .status(200)
                .message("Success")
                .users(users)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Retrieves the details of the currently authenticated user.
     *
     * @return The sanitized User object of the currently logged-in user.
     * @throws NotFoundException if the user is not found.
     */
    @Override
    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User Not Found"));
        return sanitizeUser(user);
    }

    /**
     * Retrieves a specific user by their ID.
     *
     * @param id The ID of the user.
     * @return A Response containing the sanitized user details.
     * @throws NotFoundException if the user is not found.
     */
    @Override
    public Response getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        return Response.builder()
                .status(200)
                .message("Success")
                .user(sanitizeUser(user))
                .build();
    }

    /**
     * Updates an existing user's profile details.
     *
     * @param id The ID of the user to update.
     * @param userRequest The updated user details.
     * @return A Response indicating the result of the update.
     * @throws NotFoundException if the user is not found.
     * @throws AccessDeniedException if the user is not authorized to update the profile.
     */
    @Override
    public Response updateUser(Long id, User userRequest) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        // Authorization check: users can only update their own profile unless they are ADMIN
        User caller = getCurrentLoggedInUser();
        if (!caller.getId().equals(id) && caller.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (userRequest.getEmail() != null) existingUser.setEmail(userRequest.getEmail());
        if (userRequest.getPhoneNumber() != null) existingUser.setPhoneNumber(userRequest.getPhoneNumber());
        if (userRequest.getName() != null) existingUser.setName(userRequest.getName());

        // Only ADMIN can change roles, but cannot change their own role or other admins' roles (prevents lockout)
        if (userRequest.getRole() != null) {
            if (caller.getRole() == UserRole.ADMIN) {
                // Prevent changing role of any admin (including self) to avoid lockout
                if (existingUser.getRole() != UserRole.ADMIN) {
                    existingUser.setRole(userRequest.getRole());
                }
            }
        }

        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
            authorizePasswordChange(id);
            existingUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        userRepository.update(existingUser);

        return Response.builder()
                .status(200)
                .message("User successfully updated")
                .user(sanitizeUser(existingUser))
                .build();
    }

    /**
     * Changes the password for a specific user.
     *
     * @param id The ID of the user.
     * @param currentPassword The current password for verification.
     * @param newPassword The new password to be set.
     * @return A Response indicating the result of the password change.
     * @throws NotFoundException if the user is not found.
     * @throws InvalidCredentialsException if the current password is incorrect.
     * @throws AccessDeniedException if the user is not authorized to change the password.
     */
    @Override
    public Response changePassword(Long id, String currentPassword, String newPassword) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        // Authorization check: users can only change their own password unless they are ADMIN
        authorizePasswordChange(id);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);

        return Response.builder()
                .status(200)
                .message("Password changed successfully")
                .build();
    }

    /**
     * Deletes a user by their ID (soft delete).
     *
     * @param id The ID of the user to delete.
     * @return A Response indicating the result of the deletion.
     * @throws NotFoundException if the user is not found.
     */
    @Override
    public Response deleteUser(Long id) {

        userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        userRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("User successfully deactivated")
                .build();
    }

    /**
     * Reactivates a previously deleted/deactivated user.
     *
     * @param id The ID of the user to reactivate.
     * @return A Response indicating the result of the reactivation.
     * @throws NotFoundException if the user is not found.
     */
    @Override
    public Response reactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        userRepository.activateById(id);
        user.setIsActive(true);

        return Response.builder()
                .status(200)
                .message("User successfully reactivated")
                .user(sanitizeUser(user))
                .build();
    }

    /**
     * Registers a new user in the system with STAFF role.
     *
     * @param request The registration request containing user details.
     * @return A Response indicating the result of the registration.
     * @throws NameValueRequiredException if the email is already registered.
     */
    @Override
    public Response registerUser(RegisterRequest request) {
        User user = buildAndSaveUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhoneNumber(),
                UserRole.STAFF);
        return Response.builder()
                .status(200)
                .message("Account created successfully. You may now log in.")
                .user(user)
                .build();
    }

    /**
     * Creates a new user via an administrative request.
     *
     * @param request The admin create user request.
     * @return A Response indicating the result of the user creation.
     * @throws NameValueRequiredException if the email is already registered.
     */
    @Override
    public Response createUser(AdminCreateUserRequest request) {
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.MANAGER;
        User user = buildAndSaveUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhoneNumber(),
                role);
        return Response.builder()
                .status(200)
                .message("User created successfully")
                .user(user)
                .build();
    }

    /**
     * Exports all users to a CSV format.
     *
     * @return A CSV string containing the user data.
     */
    @Override
    public String exportUsersCsv() {
        // Cap at 10 000 rows to match other export endpoints
        List<User> users = userRepository.findAll("id", "ASC").stream()
                .limit(10_000)
                .toList();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Email,Phone Number,Role,Active,Created At\n");
        for (User u : users) {
            csv.append(u.getId()).append(",");
            csv.append(CsvExportUtils.escapeCsv(u.getName())).append(",");
            csv.append(CsvExportUtils.escapeCsv(u.getEmail())).append(",");
            csv.append(CsvExportUtils.escapeCsv(u.getPhoneNumber())).append(",");
            csv.append(u.getRole() != null ? u.getRole() : "").append(",");
            csv.append(Boolean.TRUE.equals(u.getIsActive()) ? "Yes" : "No").append(",");
            csv.append(CsvExportUtils.isoDateTime(u.getCreatedAt())).append("\n");
        }
        return csv.toString();
    }

    /**
     * Retrieves all transactions associated with a specific user.
     *
     * @param id The ID of the user.
     * @return A Response containing the user and their transaction summary.
     * @throws NotFoundException if the user is not found.
     */
    @Override
    public Response getUserTransactions(Long id) {
        // assume a join or separate query in repository for this
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        // transactions fetched manually or left null for now
        return Response.builder()
                .status(200)
                .message("Success")
                .user(sanitizeUser(user))
                .build();
    }
}
