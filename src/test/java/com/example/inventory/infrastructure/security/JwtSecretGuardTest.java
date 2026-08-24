package com.example.inventory.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtSecretGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtSecretGuard.class);

    @Test
    void refusesToStartWithDefaultSecretInProdProfile() {
        runner.withPropertyValues(
                "jwt.secret=change-me-please",
                "spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startsWithStrongSecretInProdProfile() {
        runner.withPropertyValues(
                "jwt.secret=a7f3c9e2b1d4f6a8c0e2b4d6f8a0c2e4",
                "spring.profiles.active=prod")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void allowsDefaultSecretOutsideProdProfile() {
        runner.withPropertyValues(
                "jwt.secret=change-me-please",
                "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
