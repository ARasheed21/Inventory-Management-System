package com.example.inventory.infrastructure.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class UserRegistry {

    private final Map<String, RegisteredUser> registeredUsers;
    private final UserDetailsService userDetailsService;

    public UserRegistry() {
        this.registeredUsers = Map.of(
                "admin", new RegisteredUser("admin-1", "admin", "admin@example.com", "admin", List.of("ADMIN")),
                "warehouse",
                new RegisteredUser("warehouse-1", "warehouse", "warehouse@example.com", "warehouse",
                        List.of("WAREHOUSE")),
                "customer",
                new RegisteredUser("customer-1", "customer", "customer@example.com", "customer", List.of("CUSTOMER")));
        this.userDetailsService = new InMemoryUserDetailsManager(
                registeredUsers.values().stream()
                        .map(user -> User.withUsername(user.username())
                                .password(user.password())
                                .roles(user.roles().toArray(String[]::new))
                                .build())
                        .collect(Collectors.toList()));
    }

    public UserDetailsService userDetailsService() {
        return userDetailsService;
    }

    public Optional<RegisteredUser> findByUsername(String username) {
        return Optional.ofNullable(registeredUsers.get(username));
    }

    public record RegisteredUser(String id, String username, String email, String password, List<String> roles) {
    }
}
