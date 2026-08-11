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
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
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

        private final UserRegistry userRegistry;
        private final JwtAuthenticationConverter jwtAuthenticationConverter;

        @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins}")
        private String allowedOrigins;

        public SecurityConfig(UserRegistry userRegistry, JwtAuthenticationConverter jwtAuthenticationConverter) {
                this.userRegistry = userRegistry;
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
                                                                "/swagger-ui/**", "/swagger-ui.html", "/auth/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**")
                                                .hasRole("ADMIN")
                                                .anyRequest()
                                                .authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
                return userRegistry.userDetailsService();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public JwtEncoder jwtEncoder(@Value("${jwt.secret}") String jwtSecret) {
                byte[] secretBytes = jwtSecret.getBytes();
                OctetSequenceKey octetKey = new OctetSequenceKey.Builder(secretBytes).build();
                JWKSet jwkSet = new JWKSet(octetKey);
                JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
                return new NimbusJwtEncoder(jwkSource);
        }

        @Bean
        public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String jwtSecret) {
                SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
                return NimbusJwtDecoder.withSecretKey(secretKey)
                                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
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
