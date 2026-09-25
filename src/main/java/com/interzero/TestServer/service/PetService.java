package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.PetPatchRequest;
import com.interzero.TestServer.dto.PetRequest;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.error.InvalidReferenceException;
import com.interzero.TestServer.error.ResourceNotFoundException;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Pet}s.
 * <p>
 * Every method runs in a transaction, so a lookup and the write that follows it (e.g. in {@link #update}) see a
 * consistent state.
 */
@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;

    /**
     * Used to resolve the {@code ownerId} of incoming pets.
     */
    private final OwnerRepository ownerRepository;

    public PetService(@NonNull PetRepository petRepository, @NonNull OwnerRepository ownerRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Gets one page of pets.
     *
     * @param pageable The page and sort order, with sort properties already mapped to entity properties.
     * @return The requested page of pets.
     */
    @Transactional(readOnly = true)
    public Page<Pet> findAll(Pageable pageable) {
        return petRepository.findAll(pageable);
    }

    /**
     * Gets one page of the pets of an owner.
     *
     * @param ownerId  The ID of the owner.
     * @param pageable The page and sort order, with sort properties already mapped to entity properties.
     * @return The requested page of pets.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     */
    @Transactional(readOnly = true)
    public Page<Pet> findByOwner(Long ownerId, Pageable pageable) {
        if (!ownerRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Owner %d not found.".formatted(ownerId));
        }
        return petRepository.findByOwner_Id(ownerId, pageable);
    }

    /**
     * Gets a single pet.
     *
     * @param id The ID of the pet.
     * @return The pet.
     * @throws ResourceNotFoundException If no pet with this ID exists.
     */
    @Transactional(readOnly = true)
    public Pet get(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet %d not found.".formatted(id)));
    }

    /**
     * Creates a new pet.
     *
     * @param request The pet to create.
     * @return The created pet, with its generated ID.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     */
    public Pet create(PetRequest request) {
        return petRepository.save(request.applyTo(new Pet(), this::findOwnerForPet));
    }

    /**
     * Replaces all fields of an existing pet.
     *
     * @param id      The ID of the pet.
     * @param request The new state of the pet.
     * @return The updated pet.
     * @throws ResourceNotFoundException  If no pet with this ID exists.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     */
    public Pet update(Long id, PetRequest request) {
        return petRepository.save(request.applyTo(get(id), this::findOwnerForPet));
    }

    /**
     * Partially updates an existing pet. See {@link PetPatchRequest} for how missing and {@code null} fields are
     * treated.
     *
     * @param id      The ID of the pet.
     * @param request The fields to change.
     * @return The updated pet.
     * @throws ResourceNotFoundException  If no pet with this ID exists.
     * @throws InvalidReferenceException If the request refers to an owner that does not exist.
     */
    public Pet patch(Long id, PetPatchRequest request) {
        return petRepository.save(request.applyTo(get(id), this::findOwnerForPet));
    }

    /**
     * Deletes a pet.
     *
     * @param id The ID of the pet.
     * @throws ResourceNotFoundException If no pet with this ID exists.
     */
    public void delete(Long id) {
        petRepository.delete(get(id));
    }

    private Owner findOwnerForPet(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new InvalidReferenceException("Owner %d not found.".formatted(ownerId)));
    }
}
