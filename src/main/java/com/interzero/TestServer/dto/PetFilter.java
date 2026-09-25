package com.interzero.TestServer.dto;

import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Optional query parameters for filtering lists of {@link Pet}s. All given criteria must match; a missing or blank
 * parameter is ignored.
 *
 * @param name    Matches pets whose name contains this text, ignoring case.
 * @param species Matches pets of any of these species, e.g. {@code ?species=dog&species=cat} or {@code ?species=dog,cat}.
 * @param minAge  Matches pets at least this old.
 * @param maxAge  Matches pets at most this old.
 */
public record PetFilter(
        @Size(max = 100) String name,
        List<Species> species,
        @PositiveOrZero Integer minAge,
        @PositiveOrZero Integer maxAge) {

    /**
     * Checks that the age range is not empty.
     *
     * @return {@code false} if both bounds are given and {@code minAge} is greater than {@code maxAge}.
     */
    @AssertTrue(message = "minAge must not be greater than maxAge")
    public boolean isAgeRangeValid() {
        return minAge == null || maxAge == null || minAge <= maxAge;
    }
}
