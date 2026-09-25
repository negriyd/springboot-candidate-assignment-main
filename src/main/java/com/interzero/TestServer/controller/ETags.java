package com.interzero.TestServer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts between entity versions and HTTP {@code ETag} / {@code If-Match} header values.
 * <p>
 * Responses for a single resource carry its version as an {@code ETag}. A client can send that value back in
 * {@code If-Match} on {@code PUT}, {@code PATCH} or {@code DELETE}; the change is then rejected with 412 if the
 * resource has been modified in the meantime. Without {@code If-Match} the change is applied unconditionally.
 */
final class ETags {

    private ETags() {
    }

    /**
     * Formats a version as an {@code ETag} value. Spring adds the quotes.
     *
     * @param version The entity version.
     * @return The {@code ETag} value.
     */
    static String of(Long version) {
        return String.valueOf(version);
    }

    /**
     * Parses an {@code If-Match} header into the version the client expects.
     *
     * @param ifMatch The header value, e.g. {@code "3"}, or {@code null} if the header is missing.
     * @return The expected version, or {@code null} if the header is missing or {@code *} (match any version).
     * @throws ResponseStatusException 400 if the header is not a single version ETag.
     */
    static Long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || ifMatch.strip().equals("*")) {
            return null;
        }
        String value = ifMatch.strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid If-Match header '%s'; send the ETag from a previous response.".formatted(ifMatch));
        }
    }
}
