package com.interzero.TestServer.dto;

import com.interzero.TestServer.entity.Owner;
import jakarta.validation.constraints.Size;

/**
 * Optional query parameters for filtering lists of {@link Owner}s. All given criteria must match; a missing or blank
 * parameter is ignored. Each one matches values that contain the given text, ignoring case.
 *
 * @param nameFirst Text the first name must contain.
 * @param nameLast  Text the last name must contain.
 * @param address   Text the address must contain.
 */
public record OwnerFilter(
        @Size(max = 100) String nameFirst,
        @Size(max = 100) String nameLast,
        @Size(max = 255) String address) {
}
