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
 * Exports the generated OpenAPI contract to contracts/api/openapi.yaml - the shared
 * git-submodule repository consumed by frontend development (contract-first).
 *
 * Workflow after any API change:
 *   1. mvn test -Dtest=OpenApiContractExportTest
 *   2. cd contracts && git add . && git commit -m "contract: ..." && git push
 *   3. cd .. && git add contracts && git commit -m "chore: bump contracts" && git push
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

        Path target = Path.of("contracts", "api", "openapi.yaml");
        if (!Files.exists(target.getParent())) {
            // submodule not initialized (e.g., shallow CI checkout) - fall back to a local copy
            target = Path.of("docs", "api-contract", "openapi.yaml");
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, yaml);

        assertTrue(yaml.contains("openapi: 3."), "Expected an OpenAPI 3 document");
        assertTrue(yaml.contains("bearerAuth"), "Expected the bearer security scheme in the contract");
    }
}
