package com.interzero.TestServer.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.interzero.TestServer.entity.Owner;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The request body for partially updating an {@link Owner}. Only the fields present in the request are changed.
 * <p>
 * For {@code nameFirst} and {@code nameLast} a missing field or {@code null} means "leave unchanged", since these
 * fields are required and cannot be cleared. {@code address} distinguishes the two: a missing field leaves the
 * address unchanged, while an explicit {@code null} removes it.
 * <p>
 * This is a class with setters rather than a record for the same reason as {@link PetPatchRequest}: Jackson calls a
 * setter only for fields present in the JSON, which is how a missing {@code address} is told apart from {@code null}.
 */
@Getter
@Setter
public class OwnerPatchRequest {

    /**
     * The new first name of the owner, or {@code null} to keep the current one.
     */
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String nameFirst;

    /**
     * The new last name of the owner, or {@code null} to keep the current one.
     */
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String nameLast;

    /**
     * The new address, or {@code null} to remove it. Only applied if {@link #addressPresent} is set.
     */
    @Size(max = 255)
    private String address;

    /**
     * Whether {@code address} was present in the request, even if {@code null}.
     */
    @JsonIgnore
    @Setter(lombok.AccessLevel.NONE)
    private boolean addressPresent;

    /**
     * Sets the new address and records that the field was present in the request.
     *
     * @param address The new address, or {@code null} to remove it.
     */
    public void setAddress(String address) {
        this.address = address;
        this.addressPresent = true;
    }

    /**
     * Copies the fields present in this request onto the given owner.
     *
     * @param owner The owner to update.
     * @return The same owner, for chaining.
     */
    public Owner applyTo(Owner owner) {
        if (nameFirst != null) {
            owner.setNameFirst(nameFirst.strip());
        }
        if (nameLast != null) {
            owner.setNameLast(nameLast.strip());
        }
        if (addressPresent) {
            owner.setAddress(address != null ? address.strip() : null);
        }
        return owner;
    }
}
