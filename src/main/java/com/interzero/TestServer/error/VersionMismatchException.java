package com.interzero.TestServer.error;

/**
 * Thrown when a client updates or deletes a resource based on an outdated version of it (mapped to 412), i.e. the
 * {@code If-Match} header of the request does not match the current {@code ETag}. The client should fetch the
 * resource again and reapply its change.
 * <p>
 * Services throw this instead of an HTTP-specific exception; {@link GlobalExceptionHandler} maps it to a response.
 */
public class VersionMismatchException extends RuntimeException {

    public VersionMismatchException(String message) {
        super(message);
    }
}
