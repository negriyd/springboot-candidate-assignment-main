package com.interzero.TestServer;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that {@code GlobalExceptionHandler} maps exceptions to the expected status and JSON body.
 * {@link ErrorTestController} provides test-only endpoints that trigger each kind of error.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTests.ErrorTestController.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test-errors")
    static class ErrorTestController {

        @GetMapping("/unexpected")
        @CanRead
        public String unexpected() {
            throw new IllegalStateException("internal details that must not leak");
        }

        @GetMapping("/status")
        @CanRead
        public String status() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet 42 not found.");
        }

        @GetMapping("/conflict")
        @CanRead
        public String conflict() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @GetMapping("/optimistic-lock")
        @CanRead
        public String optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/typed/{id}")
        @CanRead
        public String typed(@PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/param")
        @CanRead
        public String param(@RequestParam String name) {
            return name;
        }

        @PostMapping("/body")
        @CanWrite
        public Map<String, Object> body(@RequestBody Map<String, Object> body) {
            return body;
        }
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("admin", "admin-password"));
    }

    @Test
    void unexpectedExceptionReturns500WithoutDetails() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/unexpected")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.path").value("/test-errors/unexpected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void responseStatusExceptionKeepsStatusAndReason() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/status")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pet 42 not found."));
    }

    @Test
    void optimisticLockFailureReturns409() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/optimistic-lock")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "The resource was modified by another request at the same time; fetch it again and retry."));
    }

    @Test
    void dataIntegrityViolationReturns409() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/conflict")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data."));
    }

    @Test
    void invalidPathVariableReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/typed/abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' has invalid value 'abc'; expected Long."));
    }

    @Test
    void missingRequestParameterReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/test-errors/param")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'name' is missing."));
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(asAdmin(post("/test-errors/body"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed."));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(asAdmin(post("/test-errors/body"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(asAdmin(get("/does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No endpoint found for this path."));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(asAdmin(put("/test-errors/param")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.message").value("Method 'PUT' is not supported for this endpoint."));
    }

    @Test
    void accessDeniedStillReturns403() throws Exception {
        mockMvc.perform(get("/pets").with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));
    }

    @Test
    void openApiDocsStillWork() throws Exception {
        mockMvc.perform(asAdmin(get("/v3/api-docs")))
                .andExpect(status().isOk());
    }
}
