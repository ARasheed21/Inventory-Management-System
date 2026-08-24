package com.example.inventory.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationConverter jwtAuthenticationConverter;

        @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins}")
        private String allowedOrigins;

        @Value("${jwt.use-rs256:false}")
        private boolean useRs256;

        @Value("${jwt.rsa.public-key-path:classpath:keys/public_key.pem}")
        private String rsaPublicKeyPath;

        @Value("${jwt.rsa.private-key-path:classpath:keys/private_key.pem}")
        private String rsaPrivateKeyPath;

        public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
                this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/health", "/api/info", "/v3/api-docs/**",
                                                                "/v3/api-docs.yaml",
                                                                "/swagger-ui/**", "/swagger-ui.html", "/auth/**",
                                                                "/ws/**")
                                                 .permitAll()
                                                .requestMatchers("/admin/**")
                                                .hasRole("ADMIN")
                                                .anyRequest()
                                                .authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                                                .authenticationEntryPoint(this::writeAuthError)
                                                .accessDeniedHandler(this::writeAuthError));

                return http.build();
        }

        private void writeAuthError(jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response,
                        org.springframework.security.core.AuthenticationException authException)
                        throws java.io.IOException {
                response.setStatus(401);
                response.setContentType("application/json");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(
                                com.example.inventory.web.api.ApiErrorBody.of(401, "Unauthorized",
                                                "Authentication required or token invalid", request.getRequestURI())));
        }

        private void writeAuthError(jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response,
                        org.springframework.security.access.AccessDeniedException accessDeniedException)
                        throws java.io.IOException {
                response.setStatus(403);
                response.setContentType("application/json");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(
                                com.example.inventory.web.api.ApiErrorBody.of(403, "Forbidden",
                                                "Insufficient permissions", request.getRequestURI())));
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService userDetailsService(
                        UserRegistry userRegistry) {
                return userRegistry.userDetailsService();
        }

        @Bean
        public JwtEncoder jwtEncoder(@Value("${jwt.secret:change-me-please}") String jwtSecret) throws Exception {
                if (useRs256) {
                        var privateKey = PemKeyUtils.loadPrivateKey(rsaPrivateKeyPath);
                        var publicKey = PemKeyUtils.loadPublicKey(rsaPublicKeyPath);
                        var rsaKey = new com.nimbusds.jose.jwk.RSAKey.Builder(
                                        (java.security.interfaces.RSAPublicKey) publicKey)
                                        .privateKey((java.security.interfaces.RSAPrivateKey) privateKey)
                                        .keyID("rs256-key-1")
                                        .build();
                        JWKSet jwkSet = new JWKSet(rsaKey);
                        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
                        return new NimbusJwtEncoder(jwkSource);
                } else {
                        SecretKey secretKey = deriveHs256Secret(jwtSecret);
                        OctetSequenceKey octetKey = new OctetSequenceKey.Builder(secretKey.getEncoded())
                                        .algorithm(JWSAlgorithm.HS256)
                                        .keyUse(KeyUse.SIGNATURE)
                                        .keyID("hs256-key-1")
                                        .build();
                        JWKSet jwkSet = new JWKSet(octetKey);
                        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
                        return new NimbusJwtEncoder(jwkSource);
                }
        }

        @Bean
        public JwtDecoder jwtDecoder(@Value("${jwt.secret:change-me-please}") String jwtSecret) throws Exception {
                if (useRs256) {
                        var publicKey = PemKeyUtils.loadPublicKey(rsaPublicKeyPath);
                        return NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) publicKey)
                                        .build();
                } else {
                        SecretKey secretKey = deriveHs256Secret(jwtSecret);
                        return NimbusJwtDecoder.withSecretKey(secretKey)
                                        .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                                        .build();
                }
        }

        private SecretKey deriveHs256Secret(String jwtSecret) throws NoSuchAlgorithmException {
                byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(secretBytes);
                return new SecretKeySpec(hash, "HmacSHA256");
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .toList());
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
