package com.interzero.TestServer;

import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the database itself rejects invalid data, not only the API validation. This protects data written
 * without going through the request DTOs, e.g. by {@code DataLoader} or future code.
 */
@SpringBootTest
class EntityConstraintsTests {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private static Pet validPet() {
        Pet pet = new Pet();
        pet.setName("Rex");
        pet.setSpecies(Species.dog);
        pet.setAge(3);
        return pet;
    }

    private static Owner validOwner() {
        Owner owner = new Owner();
        owner.setNameFirst("Jane");
        owner.setNameLast("Doe");
        return owner;
    }

    @Test
    void petNameIsRequired() {
        Pet pet = validPet();
        pet.setName(null);
        assertThatThrownBy(() -> petRepository.saveAndFlush(pet)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void petSpeciesIsRequired() {
        Pet pet = validPet();
        pet.setSpecies(null);
        assertThatThrownBy(() -> petRepository.saveAndFlush(pet)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void petAgeIsRequired() {
        Pet pet = validPet();
        pet.setAge(null);
        assertThatThrownBy(() -> petRepository.saveAndFlush(pet)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void petNameLengthIsLimited() {
        Pet pet = validPet();
        pet.setName("x".repeat(101));
        assertThatThrownBy(() -> petRepository.saveAndFlush(pet)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownerNamesAreRequired() {
        Owner withoutFirstName = validOwner();
        withoutFirstName.setNameFirst(null);
        assertThatThrownBy(() -> ownerRepository.saveAndFlush(withoutFirstName))
                .isInstanceOf(DataIntegrityViolationException.class);

        Owner withoutLastName = validOwner();
        withoutLastName.setNameLast(null);
        assertThatThrownBy(() -> ownerRepository.saveAndFlush(withoutLastName))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownerAddressLengthIsLimited() {
        Owner owner = validOwner();
        owner.setAddress("x".repeat(256));
        assertThatThrownBy(() -> ownerRepository.saveAndFlush(owner))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
