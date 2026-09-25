package com.interzero.TestServer.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The API representation of a {@link Pet}.
 * <p>
 * Decouples the JSON from the entity: new entity fields do not leak into the API, and the owner is included as a
 * small summary instead of the full entity (which would need lazy loading and would create a Pet → Owner → pets
 * cycle).
 *
 * @param id      The ID of the pet.
 * @param name    The name of the pet.
 * @param species The species of the pet.
 * @param age     The age of the pet in years.
 * @param ownerId The ID of the owner, or {@code null} if the pet has no owner. Same field as in the requests.
 * @param owner   A summary of the owner, or {@code null} if the pet has no owner.
 * @param version The entity version. Not part of the JSON; sent as the {@code ETag} header instead.
 */
public record PetResponse(
        Long id,
        String name,
        Species species,
        Integer age,
        Long ownerId,
        OwnerSummary owner,
        @JsonIgnore @Schema(hidden = true) Long version) {

    /**
     * The owner as shown inside a pet: enough to display who owns the pet without a second request.
     *
     * @param id        The ID of the owner.
     * @param nameFirst The first name of the owner.
     * @param nameLast  The last name of the owner.
     */
    public record OwnerSummary(Long id, String nameFirst, String nameLast) {
    }

    /**
     * Creates the response for a pet. Reads the owner, so it must be called while the owner can still be loaded
     * (i.e. inside the service transaction).
     *
     * @param pet The pet.
     * @return The response.
     */
    public static PetResponse from(Pet pet) {
        Owner owner = pet.getOwner();
        OwnerSummary ownerSummary = owner != null
                ? new OwnerSummary(owner.getId(), owner.getNameFirst(), owner.getNameLast())
                : null;
        return new PetResponse(pet.getId(), pet.getName(), pet.getSpecies(), pet.getAge(), pet.getOwnerId(),
                ownerSummary, pet.getVersion());
    }
}
