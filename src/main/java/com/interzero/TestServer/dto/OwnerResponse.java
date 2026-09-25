package com.interzero.TestServer.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.interzero.TestServer.entity.Owner;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The API representation of an {@link Owner}. The owner's pets are not included; they are available, paged, from
 * {@code GET /owners/{id}/pets}.
 *
 * @param id        The ID of the owner.
 * @param nameFirst The first name of the owner.
 * @param nameLast  The last name of the owner.
 * @param address   The address of the owner, or {@code null} if unknown.
 * @param version   The entity version. Not part of the JSON; sent as the {@code ETag} header instead.
 */
public record OwnerResponse(
        Long id,
        String nameFirst,
        String nameLast,
        String address,
        @JsonIgnore @Schema(hidden = true) Long version) {

    /**
     * Creates the response for an owner.
     *
     * @param owner The owner.
     * @return The response.
     */
    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(owner.getId(), owner.getNameFirst(), owner.getNameLast(), owner.getAddress(),
                owner.getVersion());
    }
}
