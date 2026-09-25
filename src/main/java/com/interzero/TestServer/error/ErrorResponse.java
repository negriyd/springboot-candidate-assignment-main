package com.interzero.TestServer.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The standard error body returned by the API.
 *
 * @param timestamp The moment the error occurred.
 * @param status    The HTTP status code.
 * @param error     The HTTP status reason phrase, e.g. "Unauthorized".
 * @param message   A human-readable explanation of the error.
 * @param path      The request path that caused the error.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    /**
     * Creates an error response for the given status, timestamped now.
     *
     * @param status  The HTTP status.
     * @param message A human-readable explanation of the error.
     * @param path    The request path that caused the error.
     * @return The error response.
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
