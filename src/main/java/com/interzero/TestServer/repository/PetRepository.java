package com.interzero.TestServer.repository;

import com.interzero.TestServer.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The repository that manages the Pet entity.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {

    /**
     * Gets a pet together with its owner in one query, since every pet response includes an owner summary.
     */
    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Pet> findById(Long id);

    /**
     * Gets one page of pets together with their owners in one query, avoiding one extra query per pet (N+1).
     */
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Pet> findAll(Specification<Pet> spec, Pageable pageable);

    // The underscore in the method name below makes Spring Data use the path owner.id. Without it, Spring Data
    // would match Pet.getOwnerId(), which is a convenience getter and not a JPA attribute.

    /**
     * Checks whether an owner has any pets.
     *
     * @param ownerId The ID of the owner.
     * @return {@code true} if at least one pet belongs to the owner.
     */
    boolean existsByOwner_Id(Long ownerId);
}
