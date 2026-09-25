package com.interzero.TestServer.repository;

import com.interzero.TestServer.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The repository that manages the Pet entity.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

}
