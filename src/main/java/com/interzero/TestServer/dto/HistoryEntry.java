package com.interzero.TestServer.dto;

import java.time.Instant;

/**
 * One change in the history of a resource: who changed it, when, how, and what it looked like afterwards.
 *
 * @param revision   The revision number. Increases with every change to any audited resource, so it orders changes
 *                   across resources too.
 * @param timestamp  When the change was committed.
 * @param username   The user who made the change, or {@code system} for changes made by the application itself.
 * @param changeType Whether the resource was created, updated or deleted.
 * @param version    The resource version after the change; matches the {@code ETag} the API returned at that time.
 * @param state      The resource after the change. For a deletion, its last state before it was deleted.
 * @param <T>        The resource type.
 */
public record HistoryEntry<T>(
        long revision,
        Instant timestamp,
        String username,
        ChangeType changeType,
        Long version,
        T state) {

    /**
     * The kind of change.
     */
    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
