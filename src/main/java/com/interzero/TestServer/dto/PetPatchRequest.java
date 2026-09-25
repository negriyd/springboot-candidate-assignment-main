package com.interzero.TestServer.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The request body for partially updating a {@link Pet}. Only the fields present in the request are changed.
 * <p>
 * For {@code name}, {@code species} and {@code age} a missing field or {@code null} means "leave unchanged", since
 * these fields are required and cannot be cleared. {@code ownerId} distinguishes the two: a missing field leaves the
 * owner unchanged, while an explicit {@code null} removes the owner.
 * <p>
 * This is a class with setters rather than a record on purpose: Jackson calls a setter only for fields present in
 * the JSON, which is how a missing {@code ownerId} is told apart from an explicit {@code null}. A record is created
 * through its constructor, where both cases arrive as the same value.
 */
@Getter
@Setter
public class PetPatchRequest {

    /**
     * The new name of the pet, or {@code null} to keep the current one.
     */
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String name;

    /**
     * The new species of the pet, or {@code null} to keep the current one.
     */
    private Species species;

    /**
     * The new age of the pet in years, or {@code null} to keep the current one.
     */
    @PositiveOrZero
    @Max(100)
    private Integer age;

    /**
     * The new owner ID, or {@code null} to remove the owner. Only applied if {@link #ownerIdPresent} is set.
     */
    private Long ownerId;

    /**
     * Whether {@code ownerId} was present in the request, even if {@code null}.
     */
    @JsonIgnore
    @Setter(lombok.AccessLevel.NONE)
    private boolean ownerIdPresent;

    /**
     * Sets the new owner ID and records that the field was present in the request.
     *
     * @param ownerId The new owner ID, or {@code null} to remove the owner.
     */
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        this.ownerIdPresent = true;
    }

    /**
     * Copies the fields present in this request onto the given pet.
     *
     * @param pet The pet to update.
     * @return The same pet, for chaining.
     */
    public Pet applyTo(Pet pet) {
        if (name != null) {
            pet.setName(name.strip());
        }
        if (species != null) {
            pet.setSpecies(species);
        }
        if (age != null) {
            pet.setAge(age);
        }
        if (ownerIdPresent) {
            pet.setOwnerId(ownerId);
        }
        return pet;
    }
}
