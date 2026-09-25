package com.interzero.TestServer;

import lombok.extern.slf4j.Slf4j;
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
     * @return A greeting.
     */
    @GetMapping
    public String hello() {
        log.info("HelloController.hello() called");
        return "Hello, World!";
    }
}
