package com.interzero.TestServer.dto;

import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.function.Function;

/**
 * The request body for creating or replacing a {@link Pet}.
 * <p>
 * The ID is deliberately not part of the request: it is generated on create and taken from the path on update.
 *
 * @param name    The name of the pet.
 * @param species The species of the pet.
 * @param age     The age of the pet in years.
 * @param ownerId The ID of the owner, or {@code null} if the pet has no owner.
 */
public record PetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Species species,
        @NotNull @PositiveOrZero @Max(100) Integer age,
        Long ownerId) {

    /**
     * Copies the fields of this request onto the given pet.
     *
     * @param pet         The pet to update.
     * @param ownerLookup Resolves an owner ID to an owner, or {@code null} to {@code null}.
     * @return The same pet, for chaining.
     */
    public Pet applyTo(Pet pet, Function<Long, Owner> ownerLookup) {
        pet.setName(name.strip());
        pet.setSpecies(species);
        pet.setAge(age);
        pet.setOwner(ownerLookup.apply(ownerId));
        return pet;
    }
}
