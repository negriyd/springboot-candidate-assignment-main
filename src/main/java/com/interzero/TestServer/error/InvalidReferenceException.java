package com.interzero.TestServer.error;

/**
 * Thrown when a request body refers to a resource that does not exist (mapped to 400), e.g. an unknown
 * {@code ownerId} when saving a pet. Unlike {@link ResourceNotFoundException}, the resource addressed by the URL
 * exists; only a reference in the body is invalid.
 * <p>
 * Services throw this instead of an HTTP-specific exception; {@link GlobalExceptionHandler} maps it to a response.
 */
public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String message) {
        super(message);
    }
}
