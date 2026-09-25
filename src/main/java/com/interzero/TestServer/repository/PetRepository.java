package com.interzero.TestServer.repository;

import com.interzero.TestServer.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The repository that manages the Pet entity.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    // The underscore in the method names below makes Spring Data use the path owner.id. Without it, Spring Data
    // would match Pet.getOwnerId(), which is only a JSON getter and not a JPA attribute.

    /**
     * Gets one page of the pets of an owner.
     *
     * @param ownerId  The ID of the owner.
     * @param pageable The requested page and sort order.
     * @return The requested page of pets.
     */
    Page<Pet> findByOwner_Id(Long ownerId, Pageable pageable);

    /**
     * Checks whether an owner has any pets.
     *
     * @param ownerId The ID of the owner.
     * @return {@code true} if at least one pet belongs to the owner.
     */
    boolean existsByOwner_Id(Long ownerId);
}
