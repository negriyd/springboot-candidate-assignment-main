package com.interzero.TestServer.repository;

import com.interzero.TestServer.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * The repository that manages the Pet entity.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {

    // The underscore in the method name below makes Spring Data use the path owner.id. Without it, Spring Data
    // would match Pet.getOwnerId(), which is only a JSON getter and not a JPA attribute.

    /**
     * Checks whether an owner has any pets.
     *
     * @param ownerId The ID of the owner.
     * @return {@code true} if at least one pet belongs to the owner.
     */
    boolean existsByOwner_Id(Long ownerId);
}
