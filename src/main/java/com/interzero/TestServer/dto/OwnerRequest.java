package com.interzero.TestServer.dto;

import com.interzero.TestServer.entity.Owner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The request body for creating or replacing an {@link Owner}.
 * <p>
 * The ID is deliberately not part of the request: it is generated on create and taken from the path on update.
 * Pets are not part of it either; a pet is assigned to an owner through the pet's {@code ownerId}.
 *
 * @param nameFirst The first name of the owner.
 * @param nameLast  The last name of the owner.
 * @param address   The address of the owner, or {@code null} if unknown.
 */
public record OwnerRequest(
        @NotBlank @Size(max = 100) String nameFirst,
        @NotBlank @Size(max = 100) String nameLast,
        @Size(max = 255) String address) {

    /**
     * Copies the fields of this request onto the given owner.
     *
     * @param owner The owner to update.
     * @return The same owner, for chaining.
     */
    public Owner applyTo(Owner owner) {
        owner.setNameFirst(nameFirst.strip());
        owner.setNameLast(nameLast.strip());
        owner.setAddress(address != null ? address.strip() : null);
        return owner;
    }
}
