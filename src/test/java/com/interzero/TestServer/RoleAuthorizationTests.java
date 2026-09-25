package com.interzero.TestServer;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the role rules: reader reads only, writer writes only, admin does everything.
 * <p>
 * The API has no write endpoints yet, so {@link AccessTestController} provides test-only endpoints
 * protected by {@link CanRead} and {@link CanWrite}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RoleAuthorizationTests.AccessTestController.class)
class RoleAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test-access")
    static class AccessTestController {

        @GetMapping
        @CanRead
        public String read() {
            return "read";
        }

        @PostMapping
        @CanWrite
        public String create() {
            return "created";
        }

        @DeleteMapping
        @CanWrite
        public String delete() {
            return "deleted";
        }
    }

    @Test
    void readerCanRead() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/test-access").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
    }

    @Test
    void readerCannotWrite() throws Exception {
        mockMvc.perform(post("/test-access").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(delete("/test-access").with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writerCannotRead() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/test-access").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writerCanWrite() throws Exception {
        mockMvc.perform(post("/test-access").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));
        mockMvc.perform(delete("/test-access").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanReadAndWrite() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/test-access").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/test-access").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
    }

    @Test
    void everyRoleCanUseHealthCheck() throws Exception {
        for (String user : new String[]{"reader", "writer", "admin"}) {
            mockMvc.perform(get("/").with(httpBasic(user, user + "-password")))
                    .andExpect(status().isOk());
        }
    }
}
