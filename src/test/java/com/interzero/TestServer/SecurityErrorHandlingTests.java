package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void h2ConsoleRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void h2ConsoleIsForbiddenForReaderAndWriter() throws Exception {
        mockMvc.perform(get("/h2-console/").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/h2-console/").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void h2ConsoleIsAllowedForAdmin() throws Exception {
        // MockMvc runs only the DispatcherServlet, not the H2 console servlet, so the request itself ends in 404.
        // What matters here is that security lets it through.
        mockMvc.perform(get("/h2-console/").with(httpBasic("admin", "admin-password")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void framesAreAllowedFromSameOriginOnly() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("admin", "admin-password")))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }
}
