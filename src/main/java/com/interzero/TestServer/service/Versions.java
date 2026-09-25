package com.interzero.TestServer.service;

import com.interzero.TestServer.error.VersionMismatchException;

/**
 * Helpers for optimistic locking across requests: the client sends back the version it read ({@code If-Match}), and
 * the change is rejected if the resource has been modified since.
 */
final class Versions {

    private Versions() {
    }

    /**
     * Checks that a resource still has the version the client expects.
     *
     * @param resource        The resource type for the error message, e.g. {@code "Pet"}.
     * @param id              The resource ID for the error message.
     * @param currentVersion  The current version of the resource.
     * @param expectedVersion The version the client read, or {@code null} to skip the check.
     * @throws VersionMismatchException If the versions differ.
     */
    static void check(String resource, Long id, Long currentVersion, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(currentVersion)) {
            throw new VersionMismatchException(
                    "%s %d has been modified since version %d (current version: %d); fetch it again and retry."
                            .formatted(resource, id, expectedVersion, currentVersion));
        }
    }
}
