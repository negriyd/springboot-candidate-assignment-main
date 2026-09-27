package com.interzero.TestServer.entity;

/**
 * An entity with a generated ID and an optimistic-locking version ({@code @Version}). Lets services handle lookups,
 * {@code If-Match} checks and history the same way for every entity.
 */
public interface VersionedEntity {

    /**
     * @return The ID, or {@code null} before the entity is first saved.
     */
    Long getId();

    /**
     * @return The version, incremented on every update; exposed to clients as the {@code ETag}.
     */
    Long getVersion();
}
