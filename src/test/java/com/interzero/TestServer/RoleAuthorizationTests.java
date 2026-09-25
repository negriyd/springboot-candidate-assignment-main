package com.interzero.TestServer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the role rules: reader reads only, writer writes only, admin does everything.
 * <p>
 * There are no write endpoints yet, so an allowed write request reaches Spring MVC and gets
 * 405 Method Not Allowed, while a forbidden one is stopped by security with 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readerCanRead() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
    }

    @Test
    void readerCannotWrite() throws Exception {
        mockMvc.perform(post("/pets").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(delete("/pets/1").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writerCannotRead() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void writerCanWrite() throws Exception {
        mockMvc.perform(post("/pets").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void adminCanReadAndWrite() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/pets").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void everyRoleCanUseHealthCheck() throws Exception {
        for (String user : new String[]{"reader", "writer", "admin"}) {
            mockMvc.perform(get("/").with(httpBasic(user, user + "-password")))
                    .andExpect(status().isOk());
        }
    }
}
