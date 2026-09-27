package com.interzero.TestServer.dto;

/**
 * An API response for a single versioned resource. Lets controllers build the {@code Location} and {@code ETag}
 * headers the same way for every resource.
 */
public interface VersionedResponse {

    /**
     * @return The ID of the resource.
     */
    Long id();

    /**
     * @return The version of the resource, sent as the {@code ETag} header.
     */
    Long version();
}
