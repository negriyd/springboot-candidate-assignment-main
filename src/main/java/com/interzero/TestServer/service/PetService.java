package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.PetFilter;
import com.interzero.TestServer.dto.PetPatchRequest;
import com.interzero.TestServer.dto.PetRequest;
import com.interzero.TestServer.dto.PetResponse;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.error.InvalidReferenceException;
import com.interzero.TestServer.error.ResourceNotFoundException;
import com.interzero.TestServer.error.VersionMismatchException;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Pet}s. Reading one pet, deleting and the change history come from
 * {@link AbstractEntityService}; this class adds the pet lists, and create and update, which resolve the
 * {@code ownerId} of the request to an owner.
 */
@Service
@Transactional
public class PetService extends AbstractEntityService<Pet, PetResponse> {

    private final PetRepository petRepository;

    /**
     * Used to resolve the {@code ownerId} of incoming pets.
     */
    private final OwnerRepository ownerRepository;

    public PetService(@NonNull PetRepository petRepository, @NonNull OwnerRepository ownerRepository) {
        super(petRepository, Pet.class, PetResponse::from);
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Gets one page of the pets that match a filter.
     *
     * @param filter   The filter; missing criteria are ignored.
     * @param ownerId  If not {@code null}, only pets of this owner are returned. Not checked for existence.
     * @param hasOwner If not {@code null}, only pets with an owner ({@code true}) or without one ({@code false}).
     * @param pageable The page and sort order, with sort properties already mapped to entity properties.
     * @return The requested page of pets.
     */
    @Transactional(readOnly = true)
    public Page<PetResponse> findAll(PetFilter filter, Long ownerId, Boolean hasOwner, Pageable pageable) {
        return petRepository.findAll(Specifications.pets(filter, ownerId, hasOwner), pageable).map(this::toResponse);
    }

    /**
     * Gets one page of the pets of an owner that match a filter.
     *
     * @param ownerId  The ID of the owner.
     * @param filter   The filter; missing criteria are ignored.
     * @param pageable The page and sort order, with sort properties already mapped to entity properties.
     * @return The requested page of pets.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     */
    @Transactional(readOnly = true)
    public Page<PetResponse> findByOwner(Long ownerId, PetFilter filter, Pageable pageable) {
        if (!ownerRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Owner %d not found.".formatted(ownerId));
        }
        return findAll(filter, ownerId, null, pageable);
    }

    /**
     * Creates a new pet.
     *
     * @param request The pet to create.
     * @return The created pet, with its generated ID.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     */
    public PetResponse create(PetRequest request) {
        return saveAndMap(request.applyTo(new Pet(), this::findOwnerForPet));
    }

    /**
     * Replaces all fields of an existing pet.
     *
     * @param id              The ID of the pet.
     * @param request         The new state of the pet.
     * @param expectedVersion The version the client last read ({@code If-Match}), or {@code null} to skip
     *                        the check.
     * @return The updated pet.
     * @throws ResourceNotFoundException If no pet with this ID exists.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     * @throws VersionMismatchException  If {@code expectedVersion} does not match the current version.
     */
    public PetResponse update(Long id, PetRequest request, Long expectedVersion) {
        return saveAndMap(request.applyTo(findForUpdate(id, expectedVersion), this::findOwnerForPet));
    }

    /**
     * Partially updates an existing pet. See {@link PetPatchRequest} for how missing and {@code null} fields are
     * treated.
     *
     * @param id              The ID of the pet.
     * @param request         The fields to change.
     * @param expectedVersion The version the client last read ({@code If-Match}), or {@code null} to skip
     *                        the check.
     * @return The updated pet.
     * @throws ResourceNotFoundException If no pet with this ID exists.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     * @throws VersionMismatchException  If {@code expectedVersion} does not match the current version.
     */
    public PetResponse patch(Long id, PetPatchRequest request, Long expectedVersion) {
        return saveAndMap(request.applyTo(findForUpdate(id, expectedVersion), this::findOwnerForPet));
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
                .orElseThrow(() -> new InvalidReferenceException("Owner %d not found.".formatted(ownerId)));
    }
}
