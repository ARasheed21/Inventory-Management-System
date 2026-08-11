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
import com.example.inventory.web.dto.AuthResponse;
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return userRegistry.findByUsername(request.username())
                    .map(jwtService::generateTokens)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            String token = request.refreshToken();
            var jwt = jwtService.decodeToken(token);
            String username = jwt.getSubject();
            return userRegistry.findByUsername(username)
                    .map(user -> jwtService.refreshTokens(token, user))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.badRequest().build());
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
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
                        "roles", user.roles())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
