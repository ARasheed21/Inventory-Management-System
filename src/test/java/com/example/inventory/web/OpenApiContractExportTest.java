package com.example.inventory.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exports the generated OpenAPI contract to docs/api-contract/openapi.yaml so frontend
 * development can proceed against a committed, reviewable artifact (contract-first).
 *
 * Run `mvn test -Dtest=OpenApiContractExportTest` after any API change, then review the
 * updated contract file in the same commit as the API change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportsOpenApiContractToDocs() throws Exception {
        String yaml = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Path target = Path.of("docs", "api-contract", "openapi.yaml");
        Files.createDirectories(target.getParent());
        Files.writeString(target, yaml);

        assertTrue(yaml.contains("openapi: 3."), "Expected an OpenAPI 3 document");
        assertTrue(yaml.contains("bearerAuth"), "Expected the bearer security scheme in the contract");
    }
}
