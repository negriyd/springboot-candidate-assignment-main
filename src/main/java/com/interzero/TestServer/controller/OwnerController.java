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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST controller for {@link com.interzero.TestServer.entity.Owner}s. Returns {@link OwnerResponse}s, never
 * entities.
 * <p>
 * {@code GET}, {@code DELETE}, history and as-of for a single owner come from {@link AbstractEntityController}; this
 * class adds the owner list, the owner's pets, and create and update. Deleting an owner who still has pets returns
 * 409 (see {@link OwnerService}).
 */
@RestController
@RequestMapping("owners")
public class OwnerController extends AbstractEntityController<OwnerResponse> {

    private final OwnerService ownerService;

    /**
     * Used to list an owner's pets.
     */
    private final PetService petService;

    public OwnerController(@NonNull OwnerService ownerService, @NonNull PetService petService) {
        super(ownerService);
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
                                                 @ParameterObject @PageableDefault(size = 20, sort = "id")
                                                 Pageable pageable) {
        log.debug("getOwners({}, {}) called", filter, pageable);
        return PageResponse.of(ownerService.findAll(filter, Paging.mapSort(pageable, SortableFields.OWNERS)));
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
                                                  @ParameterObject @PageableDefault(size = 20, sort = "id")
                                                  Pageable pageable) {
        log.debug("getOwnerPets({}, {}, {}) called", id, filter, pageable);
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
        log.debug("createOwner() called");
        return created(ownerService.create(request));
    }

    /**
     * Replaces all fields of an existing owner. The owner's pets are not affected.
     *
     * @param id      The ID of the owner.
     * @param ifMatch Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request The new state of the owner.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PutMapping("/{id}")
    @CanWrite
    public ResponseEntity<OwnerResponse> updateOwner(@PathVariable Long id,
                                                     @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                                     String ifMatch,
                                                     @Valid @RequestBody OwnerRequest request) {
        log.debug("updateOwner({}) called", id);
        return ok(ownerService.update(id, request, ETags.parseIfMatch(ifMatch)));
    }

    /**
     * Partially updates an existing owner: only the fields present in the request are changed.
     * See {@link OwnerPatchRequest} for how missing and {@code null} fields are treated. The owner's pets are not
     * affected.
     * <p>
     * Accepts {@code application/json} and {@code application/merge-patch+json} (RFC 7396), whose semantics match.
     *
     * @param id      The ID of the owner.
     * @param ifMatch Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request The fields to change.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    @CanWrite
    public ResponseEntity<OwnerResponse> patchOwner(@PathVariable Long id,
                                                    @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                                    String ifMatch,
                                                    @Valid @RequestBody OwnerPatchRequest request) {
        log.debug("patchOwner({}) called", id);
        return ok(ownerService.patch(id, request, ETags.parseIfMatch(ifMatch)));
    }
}
