package com.example.inventory.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.infrastructure.security.JwtService;
import com.example.inventory.infrastructure.security.LoginRateLimiter;
import com.example.inventory.infrastructure.security.UserRegistry;
import com.example.inventory.web.dto.LoginRequest;
import com.example.inventory.web.dto.RefreshRequest;
import com.example.inventory.web.dto.RegisterRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh, and current-user endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRegistry userRegistry;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthenticationManager authenticationManager, UserRegistry userRegistry,
            JwtService jwtService, PasswordEncoder passwordEncoder, LoginRateLimiter loginRateLimiter) {
        this.authenticationManager = authenticationManager;
        this.userRegistry = userRegistry;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account",
            description = "Creates an account with ROLE_CUSTOMER and returns tokens immediately. "
                    + "Passwords must be at least 8 characters and contain a letter and a digit.")
    @ApiResponse(responseCode = "201", description = "Account created; returns username plus access/refresh tokens",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.example.inventory.web.dto.RegisterResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed (blank fields, bad email, weak password)",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.example.inventory.web.dto.ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Username or email already registered")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRegistry.existsByUsernameOrEmail(request.username(), request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(com.example.inventory.web.api.ApiErrorBody.of(409, "Conflict",
                            "Username or email already registered", "/auth/register"));
        }
        UserRegistry.RegisteredUser user = userRegistry.register(
                request.username(), request.email(), request.password(), passwordEncoder);
        var tokens = jwtService.generateTokens(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new com.example.inventory.web.dto.RegisterResponse(
                        user.username(),
                        tokens.accessToken(),
                        tokens.refreshToken(),
                        tokens.expiresIn()));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password",
            description = "Returns JWT access + refresh tokens. Rate limited per username: after "
                    + "security.login.max-attempts failures within the window, requests return 429 until it expires.")
    @ApiResponse(responseCode = "200", description = "Authenticated; returns AuthResponse")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    @ApiResponse(responseCode = "429", description = "Too many failed attempts; account temporarily locked")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (loginRateLimiter.isLocked(request.username())) {
            return tooManyAttempts();
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            var user = userRegistry.findByUsername(request.username());
            if (user.isEmpty()) {
                return unauthorized("/auth/login", "Unknown user");
            }
            return ResponseEntity.ok(jwtService.generateTokens(user.get()));
        } catch (AuthenticationException ex) {
            loginRateLimiter.recordFailure(request.username());
            return unauthorized("/auth/login", "Invalid username or password");
        }
    }

    private ResponseEntity<Map<String, Object>> tooManyAttempts() {
        return ResponseEntity.status(429)
                .body(Map.of(
                        "status", 429,
                        "error", "Too Many Requests",
                        "message", "Too many failed login attempts. Try again later.",
                        "path", "/auth/login"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "Validates the refresh JWT and issues a new access/refresh token pair for its subject.")
    @ApiResponse(responseCode = "200", description = "New tokens issued")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            String token = request.refreshToken();
            var jwt = jwtService.decodeToken(token);
            String username = jwt.getSubject();
            var user = userRegistry.findByUsername(username);
            if (user.isEmpty()) {
                return unauthorized("/auth/refresh", "Invalid refresh token");
            }
            return ResponseEntity.ok(jwtService.refreshTokens(token, user.get()));
        } catch (Exception ex) {
            return unauthorized("/auth/refresh", "Invalid refresh token");
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile",
            description = "Resolves the authenticated principal and returns id, username, email, and ROLE_-prefixed roles.")
    @ApiResponse(responseCode = "200", description = "Profile of the authenticated user")
    @ApiResponse(responseCode = "401", description = "No valid bearer token")
    public ResponseEntity<Map<String, Object>> me(
            @Parameter(hidden = true) Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        return userRegistry.findByUsername(principal.getName())
                .map(user -> ResponseEntity.ok(Map.of(
                        "userId", user.id(),
                        "username", user.username(),
                        "email", user.email(),
                        "roles", user.roles().stream()
                                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                .toList())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String path, String message) {
        return ResponseEntity.status(401)
                .body(com.example.inventory.web.api.ApiErrorBody.of(401, "Unauthorized", message, path));
    }
}
