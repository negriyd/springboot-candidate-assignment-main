package com.interzero.TestServer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A simple controller that returns a greeting. Use this to test that the server is running and that you are providing
 * the correct port and authentication credentials.
 */
@Slf4j
@RestController
public class HelloController {

    /**
     * Returns a greeting.
     * If you are able to access this endpoint, then you have successfully started the server and provided the correct
     * port and authentication credentials.
     *
     * Available to any authenticated user, regardless of role.
     *
     * @return A greeting.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String hello() {
        log.debug("HelloController.hello() called");
        return "Hello, World!";
    }
}
