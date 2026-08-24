package com.example.inventory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.login.max-attempts=3",
        "security.login.window-seconds=60"
})
class LoginRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void locksAccountAfterRepeatedFailedLoginsEvenWithCorrectPassword() throws Exception {
        int configuredMaxAttempts = 3;
        for (int i = 0; i < configuredMaxAttempts; i++) {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "username", "customer",
                    "password", "wrong-" + i));
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "customer", "password": "customer"}
                        """))
                .andExpect(status().isTooManyRequests());
    }
}
