package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import com.interzero.TestServer.dto.OwnerPatchRequest;
import com.interzero.TestServer.dto.OwnerRequest;
import com.interzero.TestServer.dto.PageResponse;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.service.OwnerService;
import com.interzero.TestServer.service.PetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * The REST controller for all things related to {@link Owner}s.
 * <p>
 * Handles HTTP concerns only (mapping, status codes, headers); business logic lives in {@link OwnerService} and,
 * for an owner's pets, {@link PetService}. Errors are thrown by the services and turned into responses by
 * {@link com.interzero.TestServer.error.GlobalExceptionHandler}.
 */
@Slf4j
@RestController
@RequestMapping("owners")
public class OwnerController {

    /**
     * The fields owners can be sorted by: API field name to entity property path.
     */
    static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id", "nameFirst", "nameFirst", "nameLast", "nameLast", "address", "address");

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
     * Gets one page of the owners in the database.
     * <p>
     * Paging and sorting use the standard query parameters, e.g. {@code ?page=0&size=20&sort=nameLast,asc}.
     * Sortable fields: {@code id}, {@code nameFirst}, {@code nameLast}, {@code address}.
     * Defaults to the first 20 owners sorted by ID; the page size is capped at 100.
     *
     * @param pageable The requested page and sort order.
     * @return The requested page of owners.
     */
    @GetMapping
    @CanRead
    public PageResponse<Owner> getOwners(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.info("OwnerController.getOwners({}) called", pageable);
        return PageResponse.of(ownerService.findAll(Paging.mapSort(pageable, SORTABLE_FIELDS)));
    }

    /**
     * Gets a single owner.
     *
     * @param id The ID of the owner.
     * @return The owner, or 404 if no owner with this ID exists.
     */
    @GetMapping("/{id}")
    @CanRead
    public Owner getOwner(@PathVariable Long id) {
        log.info("OwnerController.getOwner({}) called", id);
        return ownerService.get(id);
    }

    /**
     * Gets one page of the pets of an owner. Paging and sorting work as for {@code GET /pets}.
     *
     * @param id       The ID of the owner.
     * @param pageable The requested page and sort order.
     * @return The requested page of the owner's pets, or 404 if no owner with this ID exists.
     */
    @GetMapping("/{id}/pets")
    @CanRead
    public PageResponse<Pet> getOwnerPets(@PathVariable Long id,
                                          @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.info("OwnerController.getOwnerPets({}, {}) called", id, pageable);
        return PageResponse.of(petService.findByOwner(id, Paging.mapSort(pageable, PetController.SORTABLE_FIELDS)));
    }

    /**
     * Creates a new owner.
     *
     * @param request The owner to create.
     * @return 201 with the created owner and a {@code Location} header pointing to it.
     */
    @PostMapping
    @CanWrite
    public ResponseEntity<Owner> createOwner(@Valid @RequestBody OwnerRequest request) {
        log.info("OwnerController.createOwner() called");
        Owner created = ownerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Replaces all fields of an existing owner. The owner's pets are not affected.
     *
     * @param id      The ID of the owner.
     * @param request The new state of the owner.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PutMapping("/{id}")
    @CanWrite
    public Owner updateOwner(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        log.info("OwnerController.updateOwner({}) called", id);
        return ownerService.update(id, request);
    }

    /**
     * Partially updates an existing owner: only the fields present in the request are changed.
     * See {@link OwnerPatchRequest} for how missing and {@code null} fields are treated. The owner's pets are not
     * affected.
     * <p>
     * Accepts {@code application/json} and {@code application/merge-patch+json} (RFC 7396), whose semantics match.
     *
     * @param id      The ID of the owner.
     * @param request The fields to change.
     * @return The updated owner, or 404 if no owner with this ID exists.
     */
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    @CanWrite
    public Owner patchOwner(@PathVariable Long id, @Valid @RequestBody OwnerPatchRequest request) {
        log.info("OwnerController.patchOwner({}) called", id);
        return ownerService.patch(id, request);
    }

    /**
     * Deletes an owner. Fails with 409 if the owner still has pets; they must be reassigned or deleted first.
     *
     * @param id The ID of the owner.
     */
    @DeleteMapping("/{id}")
    @CanWrite
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwner(@PathVariable Long id) {
        log.info("OwnerController.deleteOwner({}) called", id);
        ownerService.delete(id);
    }
}
