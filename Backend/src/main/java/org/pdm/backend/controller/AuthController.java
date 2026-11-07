package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.service.UserService;
import org.pdm.backend.wrappers.LoginRequest;
import org.pdm.backend.wrappers.RegisterRequest;
import org.pdm.backend.wrappers.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User login and registration endpoints")
public class AuthController {

    private final UserService userService;

    private final Map<String, Deque<Long>> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000;
    private ScheduledExecutorService cleaner;

    @PostConstruct
    public void initCleanupTask() {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanupRateLimiter, 1, 1, TimeUnit.HOURS);
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        if (cleaner != null) {
            cleaner.shutdown();
        }
    }

    private void cleanupRateLimiter() {
        long now = System.currentTimeMillis();
        loginAttempts.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            return deque.isEmpty();
        });
    }

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> attempts = loginAttempts.compute(ip, (k, deque) -> {
            if (deque == null) deque = new ConcurrentLinkedDeque<>();
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) deque.pollFirst();
            deque.addLast(now);
            return deque;
        });
        return attempts.size() > MAX_ATTEMPTS;
    }

    private String getRemoteIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Too many requests")
    public ResponseEntity<Response> loginUser(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request) {
        if (isRateLimited(getRemoteIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                Response.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message("Too many login attempts. Please try again in a minute.")
                    .build()
            );
        }
        return ResponseEntity.ok(userService.loginUser(loginRequest));
    }

    @PostMapping("/register")
    @Operation(
        summary = "Self-registration",
        description = "Creates a new STAFF account. Role is always STAFF; elevated access requires an administrator."
    )
    @ApiResponse(responseCode = "200", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Email already registered or validation error")
    public ResponseEntity<Response> registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(userService.registerUser(registerRequest));
    }

}
