package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.OwnerFilter;
import com.interzero.TestServer.dto.OwnerPatchRequest;
import com.interzero.TestServer.dto.OwnerRequest;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.error.ResourceConflictException;
import com.interzero.TestServer.error.ResourceNotFoundException;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Owner}s.
 * <p>
 * Every method runs in a transaction, so a check and the write that follows it (e.g. in {@link #delete}) see a
 * consistent state.
 */
@Service
@Transactional
public class OwnerService {

    private final OwnerRepository ownerRepository;

    /**
     * Used to check for pets before deleting an owner.
     */
    private final PetRepository petRepository;

    public OwnerService(@NonNull OwnerRepository ownerRepository, @NonNull PetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    /**
     * Gets one page of the owners that match a filter.
     *
     * @param filter   The filter; missing criteria are ignored.
     * @param pageable The page and sort order, with sort properties already mapped to entity properties.
     * @return The requested page of owners.
     */
    @Transactional(readOnly = true)
    public Page<Owner> findAll(OwnerFilter filter, Pageable pageable) {
        return ownerRepository.findAll(Specifications.owners(filter), pageable);
    }

    /**
     * Gets a single owner.
     *
     * @param id The ID of the owner.
     * @return The owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     */
    @Transactional(readOnly = true)
    public Owner get(Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner %d not found.".formatted(id)));
    }

    /**
     * Creates a new owner.
     *
     * @param request The owner to create.
     * @return The created owner, with its generated ID.
     */
    public Owner create(OwnerRequest request) {
        return ownerRepository.save(request.applyTo(new Owner()));
    }

    /**
     * Replaces all fields of an existing owner. The owner's pets are not affected.
     *
     * @param id      The ID of the owner.
     * @param request The new state of the owner.
     * @return The updated owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     */
    public Owner update(Long id, OwnerRequest request) {
        return ownerRepository.save(request.applyTo(get(id)));
    }

    /**
     * Partially updates an existing owner. See {@link OwnerPatchRequest} for how missing and {@code null} fields are
     * treated. The owner's pets are not affected.
     *
     * @param id      The ID of the owner.
     * @param request The fields to change.
     * @return The updated owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     */
    public Owner patch(Long id, OwnerPatchRequest request) {
        return ownerRepository.save(request.applyTo(get(id)));
    }

    /**
     * Deletes an owner. An owner who still has pets cannot be deleted: the pets must be reassigned (or deleted)
     * first, so that no pet is changed as a side effect.
     *
     * @param id The ID of the owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     * @throws ResourceConflictException If the owner still has pets.
     */
    public void delete(Long id) {
        Owner owner = get(id);
        if (petRepository.existsByOwner_Id(id)) {
            throw new ResourceConflictException(
                    "Owner %d still has pets; reassign or delete them first.".formatted(id));
        }
        ownerRepository.delete(owner);
    }
}
