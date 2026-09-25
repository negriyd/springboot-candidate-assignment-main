package com.interzero.TestServer.controller;

import java.util.Map;

/**
 * The fields each list endpoint can be sorted by, used with {@link Paging#mapSort}.
 * <p>
 * Each map goes from the API field name (as in the JSON and the {@code sort} parameter) to the entity property path
 * used in the query. Kept in one place so that every endpoint listing the same resource sorts the same way, e.g.
 * {@code GET /pets} and {@code GET /owners/{id}/pets}.
 */
final class SortableFields {

    /**
     * Sortable fields for lists of pets.
     */
    static final Map<String, String> PETS = Map.of(
            "id", "id", "name", "name", "species", "species", "age", "age", "ownerId", "owner.id");

    /**
     * Sortable fields for lists of owners.
     */
    static final Map<String, String> OWNERS = Map.of(
            "id", "id", "nameFirst", "nameFirst", "nameLast", "nameLast", "address", "address");

    private SortableFields() {
    }
}
