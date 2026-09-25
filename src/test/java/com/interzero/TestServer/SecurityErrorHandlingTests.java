package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityErrorHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingCredentialsReturns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication is required. Provide a username and password using HTTP Basic authentication."))
                .andExpect(jsonPath("$.path").value("/pets"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void wrongCredentialsReturns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void validCredentialsReturns200() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, World!"));
    }
}
