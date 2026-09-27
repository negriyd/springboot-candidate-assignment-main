package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that API users are defined by properties only: users can be added, can have several roles, and single
 * values can be overridden without losing the other users. The extra properties below are added on top of
 * {@code application.properties}, the same way an environment variable or a profile would be.
 */
@SpringBootTest(properties = {
        "app.security.users.auditor.password=auditor-password",
        "app.security.users.auditor.roles=reader,writer",
        "app.security.users.hashed.password={noop}hashed-password",
        "app.security.users.hashed.roles=READER",
        "app.security.users.reader.password=changed-password",
})
@AutoConfigureMockMvc
class SecurityUsersTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userAddedByPropertiesCanLogIn() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("auditor", "auditor-password")))
                .andExpect(status().isOk());
    }

    @Test
    void userWithSeveralRolesHasAllOfThem() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("auditor", "auditor-password")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/owners").with(httpBasic("auditor", "auditor-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nameFirst\": \"Jane\", \"nameLast\": \"Doe\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void preEncodedPasswordIsUsedAsIs() throws Exception {
        // If "{noop}hashed-password" were hashed again, the plain password would no longer match.
        mockMvc.perform(get("/pets").with(httpBasic("hashed", "hashed-password")))
                .andExpect(status().isOk());
    }

    @Test
    void overridingOnePasswordKeepsOtherUsers() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pets").with(httpBasic("reader", "changed-password")))
                .andExpect(status().isOk());

        // Users from application.properties that were not overridden are still there.
        mockMvc.perform(get("/pets").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isOk());
    }
}
