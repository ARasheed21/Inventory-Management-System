package com.example.inventory.infrastructure.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.inventory.infrastructure.persistence.jpa.AccountJpaEntity;
import com.example.inventory.infrastructure.persistence.jpa.AccountJpaEntityRepository;

@Component("securityUserRegistry")
public class UserRegistry {

    private final AccountJpaEntityRepository accountRepository;

    public UserRegistry(AccountJpaEntityRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Bean
    public ApplicationRunner seedAccounts(PasswordEncoder passwordEncoder) {
        return args -> {
            if (accountRepository.count() > 0) {
                return;
            }
            accountRepository.save(new AccountJpaEntity(
                    UUID.randomUUID().toString(), "admin", "admin@example.com",
                    passwordEncoder.encode("admin"), List.of("ADMIN")));
            accountRepository.save(new AccountJpaEntity(
                    UUID.randomUUID().toString(), "warehouse", "warehouse@example.com",
                    passwordEncoder.encode("warehouse"), List.of("WAREHOUSE")));
            accountRepository.save(new AccountJpaEntity(
                    UUID.randomUUID().toString(), "customer", "customer@example.com",
                    passwordEncoder.encode("customer"), List.of("CUSTOMER")));
        };
    }

    public UserDetailsService userDetailsService() {
        return username -> accountRepository.findByUsername(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
    }

    public Optional<RegisteredUser> findByUsername(String username) {
        return accountRepository.findByUsername(username).map(this::toRegisteredUser);
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        return accountRepository.existsByUsernameIgnoreCase(username)
                || accountRepository.existsByEmailIgnoreCase(email);
    }

    public RegisteredUser register(String username, String email, String rawPassword,
            PasswordEncoder passwordEncoder) {
        AccountJpaEntity account = accountRepository.save(new AccountJpaEntity(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordEncoder.encode(rawPassword),
                List.of("CUSTOMER")));
        return toRegisteredUser(account);
    }

    private UserDetails toUserDetails(AccountJpaEntity account) {
        return org.springframework.security.core.userdetails.User
                .withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRoles().toArray(String[]::new))
                .build();
    }

    private RegisteredUser toRegisteredUser(AccountJpaEntity account) {
        return new RegisteredUser(account.getExternalId(), account.getUsername(), account.getEmail(),
                account.getPasswordHash(), account.getRoles());
    }

    public record RegisteredUser(String id, String username, String email, String password, List<String> roles) {
    }
}
