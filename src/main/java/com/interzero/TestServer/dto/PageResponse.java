package com.interzero.TestServer.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of a paged list response.
 * <p>
 * Used instead of serializing Spring Data's {@link Page} directly, whose JSON shape is not a stable API.
 *
 * @param content       The items on this page.
 * @param page          The zero-based index of this page.
 * @param size          The requested page size.
 * @param totalElements The total number of items across all pages.
 * @param totalPages    The total number of pages.
 * @param <T>           The item type.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Creates a page response from a Spring Data page.
     *
     * @param page The Spring Data page.
     * @param <T>  The item type.
     * @return The page response.
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
