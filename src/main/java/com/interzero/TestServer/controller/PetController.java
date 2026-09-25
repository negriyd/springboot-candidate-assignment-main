package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.configuration.CanWrite;
import com.interzero.TestServer.dto.PageResponse;
import com.interzero.TestServer.dto.PetPatchRequest;
import com.interzero.TestServer.dto.PetRequest;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * The REST controller for all things related to {@link Pet}s.
 */
@Slf4j
@RestController
@RequestMapping("pets")
public class PetController {

    /**
     * The pet repository. This is used to interact with the database.
     */
    /**
     * The fields pets can be sorted by: API field name to entity property path.
     */
    static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id", "name", "name", "species", "species", "age", "age", "ownerId", "owner.id");

    private final PetRepository petRepository;

    /**
     * The owner repository. Used to resolve the {@code ownerId} of incoming pets.
     */
    private final OwnerRepository ownerRepository;

    public PetController(@NonNull PetRepository petRepository, @NonNull OwnerRepository ownerRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Gets one page of the pets in the database.
     * <p>
     * Paging and sorting use the standard query parameters, e.g. {@code ?page=0&size=20&sort=name,asc}.
     * Sortable fields: {@code id}, {@code name}, {@code species}, {@code age}, {@code ownerId}.
     * Defaults to the first 20 pets sorted by ID; the page size is capped at 100.
     *
     * @param pageable The requested page and sort order.
     * @return The requested page of pets.
     */
    @GetMapping
    @CanRead
    public PageResponse<Pet> getPets(@ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.info("PetController.getPets({}) called", pageable);
        return PageResponse.of(petRepository.findAll(Paging.mapSort(pageable, SORTABLE_FIELDS)));
    }

    /**
     * Gets a single pet.
     *
     * @param id The ID of the pet.
     * @return The pet.
     * @throws ResponseStatusException 404 if no pet with this ID exists.
     */
    @GetMapping("/{id}")
    @CanRead
    public Pet getPet(@PathVariable Long id) {
        log.info("PetController.getPet({}) called", id);
        return findPet(id);
    }

    /**
     * Creates a new pet.
     *
     * @param request The pet to create.
     * @return 201 with the created pet and a {@code Location} header pointing to it.
     */
    @PostMapping
    @CanWrite
    public ResponseEntity<Pet> createPet(@Valid @RequestBody PetRequest request) {
        log.info("PetController.createPet() called");
        Pet created = petRepository.save(request.applyTo(new Pet(), this::findOwnerForPet));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Replaces all fields of an existing pet.
     *
     * @param id      The ID of the pet.
     * @param request The new state of the pet.
     * @return The updated pet.
     * @throws ResponseStatusException 404 if no pet with this ID exists.
     */
    @PutMapping("/{id}")
    @CanWrite
    public Pet updatePet(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        log.info("PetController.updatePet({}) called", id);
        return petRepository.save(request.applyTo(findPet(id), this::findOwnerForPet));
    }

    /**
     * Partially updates an existing pet: only the fields present in the request are changed.
     * See {@link PetPatchRequest} for how missing and {@code null} fields are treated.
     * <p>
     * Accepts {@code application/json} and {@code application/merge-patch+json} (RFC 7396), whose semantics match.
     *
     * @param id      The ID of the pet.
     * @param request The fields to change.
     * @return The updated pet.
     * @throws ResponseStatusException 404 if no pet with this ID exists.
     */
    @PatchMapping(path = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    @CanWrite
    public Pet patchPet(@PathVariable Long id, @Valid @RequestBody PetPatchRequest request) {
        log.info("PetController.patchPet({}) called", id);
        return petRepository.save(request.applyTo(findPet(id), this::findOwnerForPet));
    }

    /**
     * Deletes a pet.
     *
     * @param id The ID of the pet.
     * @throws ResponseStatusException 404 if no pet with this ID exists.
     */
    @DeleteMapping("/{id}")
    @CanWrite
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePet(@PathVariable Long id) {
        log.info("PetController.deletePet({}) called", id);
        petRepository.delete(findPet(id));
    }

    private Pet findPet(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet %d not found.".formatted(id)));
    }

    /**
     * Resolves the {@code ownerId} of a pet request. An unknown owner is a client error (400), not a 404: the
     * resource addressed by the URL exists, only a reference in the body is invalid.
     */
    private Owner findOwnerForPet(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Owner %d not found.".formatted(ownerId)));
    }
}
