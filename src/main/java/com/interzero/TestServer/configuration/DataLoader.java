package com.interzero.TestServer.configuration;

import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * The data loader. This is a simple component that is used to populate the database with some data.
 */
@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final PetRepository petRepository;

    private final OwnerRepository ownerRepository;

    public DataLoader(PetRepository petRepository, OwnerRepository ownerRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Runs the data loader. This adds some owners and pets to the database, and anything else you may require.
     *
     * @param args The command line arguments.
     */
    @Override
    public void run(String... args) {
        log.info("Populating database...");

        log.info("Adding owners and pets...");
        addOwnersAndPets();

        log.info("Database populated!");
    }

    /**
     * Adds some owners and pets to the database. One owner has several pets, one has none yet,
     * and some pets have no owner. Feel free to alter this if it doesn't fit your needs.
     */
    public void addOwnersAndPets() {
        Owner alice = addOwner("Alice", "Smith", "12 Oak Street");
        addOwner("Bob", "Jones", "34 Elm Street");

        addPet("Spot", Species.dog, 2, alice);
        addPet("Mittens", Species.cat, 3, alice);
        addPet("Bun", Species.rabbit, 1, alice);
        addPet("Hammy", Species.hamster, 1, null);
        addPet("Tweety", Species.bird, 1, null);
    }

    private Owner addOwner(String nameFirst, String nameLast, String address) {
        Owner owner = new Owner();
        owner.setNameFirst(nameFirst);
        owner.setNameLast(nameLast);
        owner.setAddress(address);
        return ownerRepository.save(owner);
    }

    private void addPet(String name, Species species, int age, Owner owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setAge(age);
        pet.setOwner(owner);
        petRepository.save(pet);
    }
}
