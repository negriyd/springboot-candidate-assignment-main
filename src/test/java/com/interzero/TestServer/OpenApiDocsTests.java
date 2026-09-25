package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the API documentation: it is public, declares HTTP Basic authentication (the "Authorize" button in
 * Swagger UI), and making it public does not open the API itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void declaresHttpBasicForAllEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andExpect(jsonPath("$.security[0].basicAuth").isArray())
                .andExpect(jsonPath("$.info.title").value("Pet Management API"));
    }

    @Test
    void apiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/pets"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/owners"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }
}
