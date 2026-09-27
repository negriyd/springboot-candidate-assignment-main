package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import com.interzero.TestServer.dto.PageResponse;
import com.interzero.TestServer.dto.PetFilter;
import com.interzero.TestServer.dto.PetPatchRequest;
import com.interzero.TestServer.dto.PetRequest;
import com.interzero.TestServer.dto.PetResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST controller for {@link com.interzero.TestServer.entity.Pet}s. Returns {@link PetResponse}s, never entities.
 * <p>
 * {@code GET}, {@code DELETE}, history and as-of for a single pet come from {@link AbstractEntityController}; this
 * class adds the pet list and create and update.
 */
@RestController
@RequestMapping("pets")
public class PetController extends AbstractEntityController<PetResponse> {

    private final PetService petService;

    public PetController(@NonNull PetService petService) {
        super(petService);
        this.petService = petService;
    }

    /**
     * Gets one page of the pets in the database, optionally filtered.
     * <p>
     * Filters are optional and combined with AND, e.g. {@code ?name=sp&species=dog,cat&minAge=1&ownerId=3};
     * see {@link PetFilter}. {@code ?hasOwner=false} lists pets without an owner. Paging and sorting use the standard
     * query parameters, e.g. {@code ?page=0&size=20&sort=name,asc}.
     * Sortable fields: {@code id}, {@code name}, {@code species}, {@code age}, {@code ownerId}.
     * Defaults to the first 20 pets sorted by ID; the page size is capped at 100.
     *
     * @param filter   The filter criteria.
     * @param ownerId  If given, only pets of this owner are returned.
     * @param hasOwner If given, only pets with an owner ({@code true}) or without one ({@code false}).
     * @param pageable The requested page and sort order.
     * @return The requested page of pets, or 400 if a filter value is invalid.
     */
    @GetMapping
    @CanRead
    public PageResponse<PetResponse> getPets(@Valid @ParameterObject PetFilter filter,
                                             @RequestParam(required = false) Long ownerId,
                                             @RequestParam(required = false) Boolean hasOwner,
                                             @ParameterObject @PageableDefault(size = 20, sort = "id")
                                             Pageable pageable) {
        log.debug("getPets({}, ownerId={}, hasOwner={}, {}) called", filter, ownerId, hasOwner, pageable);
        return PageResponse.of(
                petService.findAll(filter, ownerId, hasOwner, Paging.mapSort(pageable, SortableFields.PETS)));
    }

    /**
     * Creates a new pet.
     *
     * @param request The pet to create.
     * @return 201 with the created pet and a {@code Location} header pointing to it,
     * or 400 if the request refers to an owner that does not exist.
     */
    @PostMapping
    @CanWrite
    public ResponseEntity<PetResponse> createPet(@Valid @RequestBody PetRequest request) {
        log.debug("createPet() called");
        return created(petService.create(request));
    }

    /**
     * Replaces all fields of an existing pet.
     *
     * @param id      The ID of the pet.
     * @param ifMatch Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request The new state of the pet.
     * @return The updated pet, 404 if no pet with this ID exists,
     * or 400 if the request refers to an owner that does not exist.
     */
    @PutMapping("/{id}")
    @CanWrite
    public ResponseEntity<PetResponse> updatePet(@PathVariable Long id,
                                                 @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                                 String ifMatch,
                                                 @Valid @RequestBody PetRequest request) {
        log.debug("updatePet({}) called", id);
        return ok(petService.update(id, request, ETags.parseIfMatch(ifMatch)));
    }

    /**
     * Partially updates an existing pet: only the fields present in the request are changed.
     * See {@link PetPatchRequest} for how missing and {@code null} fields are treated.
     * <p>
     * Accepts {@code application/json} and {@code application/merge-patch+json} (RFC 7396), whose semantics match.
     *
     * @param id      The ID of the pet.
     * @param ifMatch Optional {@code ETag} the client last read; if it no longer matches, 412.
     * @param request The fields to change.
     * @return The updated pet, 404 if no pet with this ID exists,
     * or 400 if the request refers to an owner that does not exist.
     */
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    @CanWrite
    public ResponseEntity<PetResponse> patchPet(@PathVariable Long id,
                                                @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                                String ifMatch,
                                                @Valid @RequestBody PetPatchRequest request) {
        log.debug("patchPet({}) called", id);
        return ok(petService.patch(id, request, ETags.parseIfMatch(ifMatch)));
    }
}
