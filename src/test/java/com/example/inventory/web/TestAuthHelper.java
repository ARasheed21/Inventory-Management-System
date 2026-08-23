package com.example.inventory.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAuthHelper {

    private TestAuthHelper() {
    }

    public static String obtainAccessToken(MockMvc mockMvc, ObjectMapper objectMapper, String username, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username,
                        password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.get("accessToken").asText();
    }
}
