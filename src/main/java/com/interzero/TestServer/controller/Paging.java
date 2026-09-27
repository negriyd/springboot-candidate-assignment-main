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

    /**
     * The largest page size; matches {@code spring.data.web.pageable.max-page-size}.
     */
    static final int MAX_PAGE_SIZE = 100;

    private Paging() {
    }

    /**
     * Restricts sorting to an allow-list of API field names and maps them to entity properties.
     * <p>
     * Without this, clients could sort by anything Spring Data can resolve: plain getters such as
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

    /**
     * Creates an unsorted page request from explicit {@code page} and {@code size} parameters, for lists with a fixed
     * order. The size is capped at {@link #MAX_PAGE_SIZE}, like the other list endpoints.
     *
     * @param page The zero-based page index.
     * @param size The page size.
     * @return The page request.
     * @throws ResponseStatusException 400 if {@code page} is negative or {@code size} is less than 1.
     */
    static Pageable of(int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid paging: page must be 0 or more and size must be 1 or more.");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }
}
