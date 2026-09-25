package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import com.interzero.TestServer.dto.OwnerFilter;
import com.interzero.TestServer.dto.OwnerPatchRequest;
import com.interzero.TestServer.dto.OwnerRequest;
import com.interzero.TestServer.dto.OwnerResponse;
import com.interzero.TestServer.dto.PageResponse;
import com.interzero.TestServer.dto.PetFilter;
import com.interzero.TestServer.dto.PetResponse;
import com.interzero.TestServer.service.OwnerService;
import com.interzero.TestServer.service.PetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * The REST controller for all things related to {@link com.interzero.TestServer.entity.Owner}s.
 * Returns {@link OwnerResponse}s, never entities.
 * <p>
 * Handles HTTP concerns only (mapping, status codes, headers); business logic lives in {@link OwnerService} and,
 * for an owner's pets, {@link PetService}. Errors are thrown by the services and turned into responses by
 * {@link com.interzero.TestServer.error.GlobalExceptionHandler}.
 * <p>
 * Single-resource responses carry an {@code ETag}. Sending it back in {@code If-Match} on {@code PUT},
 * {@code PATCH} or {@code DELETE} makes the change fail with 412 if someone else changed the resource first;
 * see {@link ETags}.
 */
@Slf4j
@RestController
@RequestMapping("owners")
public class OwnerController {

    private final OwnerService ownerService;

    /**
     * Used to list an owner's pets.
     */
    private final PetService petService;

    public OwnerController(@NonNull OwnerService ownerService, @NonNull PetService petService) {
        this.ownerService = ownerService;
        this.petService = petService;
    }

    /**
     * Gets one page of the owners in the database, optionally filtered.
     * <p>
     * Filters are optional and combined with AND, e.g. {@code ?nameLast=smi&address=oak}; see {@link OwnerFilter}.
     * Paging and sorting use the standard query parameters, e.g. {@code ?page=0&size=20&sort=nameLast,asc}.
     * Sortable fields: {@code id}, {@code nameFirst}, {@code nameLast}, {@code address}.
     * Defaults to the first 20 owners sorted by ID; the page size is capped at 100.
     *
     * @param filter   The filter criteria.
     * @param pageable The requested page and sort order.
     * @return The requested page of owners, or 400 if a filter value is invalid.
     */
    @GetMapping
    @CanRead
    public PageResponse<OwnerResponse> getOwners(@Valid @ParameterObject OwnerFilter filter,
                                         @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("OwnerController.getOwners({}, {}) called", filter, pageable);
        return PageResponse.of(ownerService.findAll(filter, Paging.mapSort(pageable, SortableFields.OWNERS)));
    }

    /**
     * Gets a single owner.
     *
     * @param id The ID of the owner.
     * @return The owner, or 404 if no owner with this ID exists.
     */
    @GetMapping("/{id}")
    @CanRead
    public ResponseEntity<OwnerResponse> getOwner(@PathVariable Long id) {
        log.debug("OwnerController.getOwner({}) called", id);
        return withETag(ownerService.get(id));
    }

    /**
     * Gets one page of the pets of an owner, optionally filtered. Filtering, paging and sorting work as for
     * {@code GET /pets} (without {@code ownerId}, which comes from the path).
     *
     * @param id       The ID of the owner.
     * @param filter   The filter criteria.
     * @param pageable The requested page and sort order.
     * @return The requested page of the owner's pets, 404 if no owner with this ID exists,
     * or 400 if a filter value is invalid.
     */
    @GetMapping("/{id}/pets")
    @CanRead
    public PageResponse<PetResponse> getOwnerPets(@PathVariable Long id,
                                          @Valid @ParameterObject PetFilter filter,
                                          @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("OwnerController.getOwnerPets({}, {}, {}) called", id, filter, pageable);
        return PageResponse.of(petService.findByOwner(id, filter, Paging.mapSort(pageable, SortableFields.PETS)));
    }

    /**
     * Creates a new owner.
     *
     * @param request The owner to create.
     * @return 201 with the created owner and a {@code Location} header pointing to it.
     */
    @PostMapping
    @CanWrite
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
        log.debug("OwnerController.createOwner() called");
        OwnerResponse created = ownerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).eTag(ETags.of(created.version())).body(created);
    }

    /**
     * Replaces all fields of an existing owner. The owner's pets are not affected.
     *
     * @param id       The ID of the owner.
     * @param ifMatch  Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request  The new state of the owner.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PutMapping("/{id}")
    @CanWrite
    public ResponseEntity<OwnerResponse> updateOwner(@PathVariable Long id,
                                        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                        @Valid @RequestBody OwnerRequest request) {
        log.debug("OwnerController.updateOwner({}) called", id);
        return withETag(ownerService.update(id, request, ETags.parseIfMatch(ifMatch)));
    }

    /**
     * Partially updates an existing owner: only the fields present in the request are changed.
     * See {@link OwnerPatchRequest} for how missing and {@code null} fields are treated. The owner's pets are not
     * affected.
     * <p>
     * Accepts {@code application/json} and {@code application/merge-patch+json} (RFC 7396), whose semantics match.
     *
     * @param id       The ID of the owner.
     * @param ifMatch  Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request  The fields to change.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    @CanWrite
    public ResponseEntity<OwnerResponse> patchOwner(@PathVariable Long id,
                                       @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                       @Valid @RequestBody OwnerPatchRequest request) {
        log.debug("OwnerController.patchOwner({}) called", id);
        return withETag(ownerService.patch(id, request, ETags.parseIfMatch(ifMatch)));
    }

    /**
     * Deletes an owner. Fails with 409 if the owner still has pets; they must be reassigned or deleted first.
     *
     * @param id       The ID of the owner.
     * @param ifMatch  Optional {@code ETag} the client last read; if it no longer matches, 412.
     */
    @DeleteMapping("/{id}")
    @CanWrite
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwner(@PathVariable Long id,
                          @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        log.debug("OwnerController.deleteOwner({}) called", id);
        ownerService.delete(id, ETags.parseIfMatch(ifMatch));
    }

    private static ResponseEntity<OwnerResponse> withETag(OwnerResponse owner) {
        return ResponseEntity.ok().eTag(ETags.of(owner.version())).body(owner);
    }
}
