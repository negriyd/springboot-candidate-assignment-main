package com.interzero.TestServer.error;

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
}
