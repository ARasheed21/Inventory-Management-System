package com.example.inventory.web.controllers;

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
import com.example.inventory.infrastructure.security.UserRegistry;
import com.example.inventory.web.dto.LoginRequest;
import com.example.inventory.web.dto.RefreshRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRegistry userRegistry;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, UserRegistry userRegistry,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRegistry = userRegistry;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
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
            return unauthorized("/auth/login", "Invalid username or password");
        }
    }

    @PostMapping("/refresh")
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
    public ResponseEntity<Map<String, Object>> me(Principal principal) {
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
