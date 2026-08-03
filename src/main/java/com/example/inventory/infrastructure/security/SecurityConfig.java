package com.example.inventory.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/health", "/api/info", "/v3/api-docs/**",
                                                                "/swagger-ui/**", "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/admin/**")
                                                .hasRole("ADMIN")
                                                .anyRequest()
                                                .authenticated())
                                .httpBasic(httpBasic -> {
                                });

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                return new InMemoryUserDetailsManager(
                                User.withUsername("admin")
                                                .password("{noop}admin")
                                                .roles("ADMIN")
                                                .build(),
                                User.withUsername("warehouse")
                                                .password("{noop}warehouse")
                                                .roles("WAREHOUSE")
                                                .build(),
                                User.withUsername("customer")
                                                .password("{noop}customer")
                                                .roles("CUSTOMER")
                                                .build());
        }
}
