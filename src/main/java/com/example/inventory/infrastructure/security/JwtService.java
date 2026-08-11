package com.example.inventory.infrastructure.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.example.inventory.web.dto.AuthResponse;

@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public AuthResponse generateTokens(UserRegistry.RegisteredUser user) {
        Instant now = Instant.now();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpirationSeconds))
                .claim("userId", user.id())
                .claim("email", user.email())
                .claim("roles", user.roles())
                .claim("type", "access")
                .build())).getTokenValue();

        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshTokenExpirationSeconds))
                .claim("userId", user.id())
                .claim("type", "refresh")
                .build())).getTokenValue();

        return new AuthResponse(accessToken, refreshToken, accessTokenExpirationSeconds);
    }

    public Jwt decodeToken(String token) {
        return jwtDecoder.decode(token);
    }

    public AuthResponse refreshTokens(String refreshToken, UserRegistry.RegisteredUser user) {
        Jwt jwt = decodeToken(refreshToken);
        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        return generateTokens(user);
    }
}
