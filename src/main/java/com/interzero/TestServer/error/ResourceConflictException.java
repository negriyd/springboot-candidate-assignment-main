package com.interzero.TestServer.error;

/**
 * Thrown when an operation conflicts with the current state of the data (mapped to 409), e.g. deleting an
 * owner who still has pets.
 * <p>
 * Services throw this instead of an HTTP-specific exception; {@link GlobalExceptionHandler} maps it to a response.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
