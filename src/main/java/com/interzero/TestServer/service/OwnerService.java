package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.OwnerFilter;
import com.interzero.TestServer.dto.OwnerPatchRequest;
import com.interzero.TestServer.dto.OwnerRequest;
import com.interzero.TestServer.dto.OwnerResponse;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.error.ResourceConflictException;
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
 * Business logic for {@link Owner}s. Reading one owner, deleting and the change history come from
 * {@link AbstractEntityService}; this class adds the owner list, create and update, and refuses to delete an owner
 * who still has pets.
 */
@Service
@Transactional
public class OwnerService extends AbstractEntityService<Owner, OwnerResponse> {

    private final OwnerRepository ownerRepository;

    /**
     * Used to check for pets before deleting an owner.
     */
    private final PetRepository petRepository;

    public OwnerService(@NonNull OwnerRepository ownerRepository, @NonNull PetRepository petRepository) {
        super(ownerRepository, Owner.class, OwnerResponse::from);
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
    public Page<OwnerResponse> findAll(OwnerFilter filter, Pageable pageable) {
        return ownerRepository.findAll(Specifications.owners(filter), pageable).map(this::toResponse);
    }

    /**
     * Creates a new owner.
     *
     * @param request The owner to create.
     * @return The created owner, with its generated ID.
     */
    public OwnerResponse create(OwnerRequest request) {
        return saveAndMap(request.applyTo(new Owner()));
    }

    /**
     * Replaces all fields of an existing owner. The owner's pets are not affected.
     *
     * @param id              The ID of the owner.
     * @param request         The new state of the owner.
     * @param expectedVersion The version the client last read ({@code If-Match}), or {@code null} to skip
     *                        the check.
     * @return The updated owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     * @throws VersionMismatchException  If {@code expectedVersion} does not match the current version.
     */
    public OwnerResponse update(Long id, OwnerRequest request, Long expectedVersion) {
        return saveAndMap(request.applyTo(findForUpdate(id, expectedVersion)));
    }

    /**
     * Partially updates an existing owner. See {@link OwnerPatchRequest} for how missing and {@code null} fields are
     * treated. The owner's pets are not affected.
     *
     * @param id              The ID of the owner.
     * @param request         The fields to change.
     * @param expectedVersion The version the client last read ({@code If-Match}), or {@code null} to skip
     *                        the check.
     * @return The updated owner.
     * @throws ResourceNotFoundException If no owner with this ID exists.
     * @throws VersionMismatchException  If {@code expectedVersion} does not match the current version.
     */
    public OwnerResponse patch(Long id, OwnerPatchRequest request, Long expectedVersion) {
        return saveAndMap(request.applyTo(findForUpdate(id, expectedVersion)));
    }

    /**
     * Refuses to delete an owner who still has pets: the pets must be reassigned (or deleted) first, so that no pet is
     * changed as a side effect.
     *
     * @throws ResourceConflictException If the owner still has pets.
     */
    @Override
    protected void beforeDelete(Owner owner) {
        if (petRepository.existsByOwner_Id(owner.getId())) {
            throw new ResourceConflictException(
                    "Owner %d still has pets; reassign or delete them first.".formatted(owner.getId()));
        }
    }
}
