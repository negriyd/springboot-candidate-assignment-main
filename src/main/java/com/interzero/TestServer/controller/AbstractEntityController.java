package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import com.interzero.TestServer.dto.HistoryEntry;
import com.interzero.TestServer.dto.PageResponse;
import com.interzero.TestServer.dto.VersionedResponse;
import com.interzero.TestServer.service.AbstractEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * Endpoints shared by all versioned, audited resources, relative to the subclass's {@code @RequestMapping}:
 * <ul>
 *     <li>{@code GET /{id}} - one resource, with its version as the {@code ETag};</li>
 *     <li>{@code DELETE /{id}} - delete, optionally only if {@code If-Match} matches;</li>
 *     <li>{@code GET /{id}/history} - the change history;</li>
 *     <li>{@code GET /{id}/history/as-of} - the resource at a point in time.</li>
 * </ul>
 * Subclasses add what differs per resource: the list endpoint with its filters, and create and update with their own
 * request types, using {@link #created} and {@link #ok} to build responses with the same headers.
 * <p>
 * Handles HTTP concerns only (mapping, status codes, headers); business logic lives in the
 * {@link AbstractEntityService}. Errors are thrown by the service and turned into responses by
 * {@link com.interzero.TestServer.error.GlobalExceptionHandler}.
 *
 * @param <R> The API response type.
 */
public abstract class AbstractEntityController<R extends VersionedResponse> {

    /**
     * Logger named after the concrete controller, e.g. {@code PetController}.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AbstractEntityService<?, R> service;

    /**
     * @param service The service of the resource.
     */
    protected AbstractEntityController(AbstractEntityService<?, R> service) {
        this.service = service;
    }

    /**
     * Gets a single resource. The response carries its version as the {@code ETag}.
     *
     * @param id The ID.
     * @return The resource, or 404 if no resource with this ID exists.
     */
    @GetMapping("/{id}")
    @CanRead
    public ResponseEntity<R> get(@PathVariable Long id) {
        log.debug("get({}) called", id);
        return ok(service.get(id));
    }

    /**
     * Deletes a resource. Some resources can refuse, e.g. an owner who still has pets (409).
     *
     * @param id      The ID.
     * @param ifMatch Optional {@code ETag} the client last read; if it no longer matches, 412.
     */
    @DeleteMapping("/{id}")
    @CanWrite
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        log.debug("delete({}) called", id);
        service.delete(id, ETags.parseIfMatch(ifMatch));
    }

    /**
     * Gets the change history of a resource: who changed it, when, and what it looked like after each change, newest
     * first. Also works after the resource has been deleted.
     *
     * @param id   The ID.
     * @param page The zero-based page index.
     * @param size The page size, at most 100.
     * @return One page of history entries, or 404 if no resource with this ID has ever existed.
     */
    @GetMapping("/{id}/history")
    @CanRead
    public PageResponse<HistoryEntry<R>> history(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        log.debug("history({}, page={}, size={}) called", id, page, size);
        return PageResponse.of(service.history(id, Paging.of(page, size)));
    }

    /**
     * Gets a resource as it was at a point in time, e.g. {@code ?time=2026-09-27T21:11:22Z}. Related data is also
     * shown as of that moment. The revision fields describe the resource's last change at or before that time.
     * <p>
     * The time is ISO-8601 with a time zone; a {@code +} in the offset must be URL-encoded as {@code %2B}.
     *
     * @param id   The ID.
     * @param time The point in time.
     * @return The resource at that time, or 404 if it never existed, did not exist yet, or had already been deleted.
     */
    @GetMapping("/{id}/history/as-of")
    @CanRead
    public HistoryEntry<R> asOf(@PathVariable Long id,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime time) {
        log.debug("asOf({}, {}) called", id, time);
        return service.asOf(id, time.toInstant());
    }

    /**
     * Builds a {@code 201 Created} response for a new resource, with a {@code Location} header pointing to it and its
     * version as the {@code ETag}.
     *
     * @param created The created resource.
     * @return The response.
     */
    protected ResponseEntity<R> created(R created) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).eTag(ETags.of(created.version())).body(created);
    }

    /**
     * Builds a {@code 200 OK} response for a single resource, with its version as the {@code ETag}.
     *
     * @param resource The resource.
     * @return The response.
     */
    protected ResponseEntity<R> ok(R resource) {
        return ResponseEntity.ok().eTag(ETags.of(resource.version())).body(resource);
    }
}
