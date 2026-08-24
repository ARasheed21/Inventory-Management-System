package com.example.inventory.infrastructure.security;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretGuard implements SmartLifecycle {

    static final String DEFAULT_SECRET = "change-me-please";
    private static final Set<String> PROTECTED_PROFILES = Set.of("prod", "production");

    private final String jwtSecret;
    private final Environment environment;
    private boolean running;

    public JwtSecretGuard(@Value("${jwt.secret:change-me-please}") String jwtSecret, Environment environment) {
        this.jwtSecret = jwtSecret;
        this.environment = environment;
    }

    @Override
    public void start() {
        boolean protectedProfileActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(PROTECTED_PROFILES::contains);
        if (protectedProfileActive && (jwtSecret == null || jwtSecret.isBlank()
                || DEFAULT_SECRET.equalsIgnoreCase(jwtSecret.trim()))) {
            throw new IllegalStateException(
                    "Refusing to start: jwt.secret is unset or uses the well-known default while a production profile "
                            + "is active. Configure a strong JWT_SECRET value.");
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
