package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.OwnerFilter;
import com.interzero.TestServer.dto.PetFilter;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds JPA {@link Specification}s (query criteria) from list filters.
 */
final class Specifications {

    private static final char LIKE_ESCAPE = '\\';

    private Specifications() {
    }

    /**
     * Matches pets that satisfy every criterion in the filter.
     *
     * @param filter   The filter; missing criteria are ignored.
     * @param ownerId  If not {@code null}, also requires the pet to belong to this owner.
     * @param hasOwner If not {@code null}, also requires the pet to have an owner ({@code true}) or none
     *                 ({@code false}).
     * @return The specification.
     */
    static Specification<Pet> pets(PetFilter filter, Long ownerId, Boolean hasOwner) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.name())) {
                predicates.add(containsIgnoreCase(cb, root.get("name"), filter.name()));
            }
            if (filter.species() != null && !filter.species().isEmpty()) {
                predicates.add(root.get("species").in(filter.species()));
            }
            if (filter.minAge() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("age"), filter.minAge()));
            }
            if (filter.maxAge() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("age"), filter.maxAge()));
            }
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            }
            if (hasOwner != null) {
                predicates.add(hasOwner ? cb.isNotNull(root.get("owner")) : cb.isNull(root.get("owner")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Matches owners that satisfy every criterion in the filter.
     *
     * @param filter The filter; missing criteria are ignored.
     * @return The specification.
     */
    static Specification<Owner> owners(OwnerFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.nameFirst())) {
                predicates.add(containsIgnoreCase(cb, root.get("nameFirst"), filter.nameFirst()));
            }
            if (StringUtils.hasText(filter.nameLast())) {
                predicates.add(containsIgnoreCase(cb, root.get("nameLast"), filter.nameLast()));
            }
            if (StringUtils.hasText(filter.address())) {
                predicates.add(containsIgnoreCase(cb, root.get("address"), filter.address()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Case-insensitive "contains" match. LIKE wildcards in the input ({@code %}, {@code _}) are escaped, so they
     * match literally instead of matching everything.
     */
    private static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> field, String text) {
        String escaped = text.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", LIKE_ESCAPE);
    }
}
