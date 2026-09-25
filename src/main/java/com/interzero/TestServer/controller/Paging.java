package com.interzero.TestServer.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helpers for paged list endpoints.
 */
final class Paging {

    private Paging() {
    }

    /**
     * Restricts sorting to an allow-list of API field names and maps them to entity properties.
     * <p>
     * Without this, clients could sort by anything Spring Data can resolve: JSON-only getters such as
     * {@code Pet.getOwnerId()} (fails with 500) or collections such as {@code Owner.pets} (joins and duplicates rows).
     *
     * @param pageable      The requested page, as bound from the query parameters.
     * @param sortableFields API field name to entity property path, e.g. {@code "ownerId" -> "owner.id"}.
     * @return The same page with its sort mapped to entity properties.
     * @throws ResponseStatusException 400 if a sort field is not in the allow-list.
     */
    static Pageable mapSort(Pageable pageable, Map<String, String> sortableFields) {
        Sort mapped = Sort.by(pageable.getSort().stream()
                .map(order -> {
                    String property = sortableFields.get(order.getProperty());
                    if (property == null) {
                        String allowed = sortableFields.keySet().stream().sorted().collect(Collectors.joining(", "));
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot sort by '%s'; allowed: %s.".formatted(order.getProperty(), allowed));
                    }
                    return order.withProperty(property);
                })
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mapped);
    }
}
