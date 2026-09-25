package com.interzero.TestServer.error;

/**
 * Thrown when a requested resource does not exist (mapped to 404), e.g. the pet in {@code GET /pets/{id}}.
 * <p>
 * Services throw this instead of an HTTP-specific exception; {@link GlobalExceptionHandler} maps it to a response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
