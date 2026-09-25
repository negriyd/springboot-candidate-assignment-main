package com.interzero.TestServer.configuration;

import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
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

    public DataLoader(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    /**
     * Runs the data loader. This adds some pets to the database, and anything else you may require.
     *
     * @param args The command line arguments.
     */
    @Override
    public void run(String... args) {
        log.info("Populating database...");

        log.info("Adding pets...");
        addPets();

        log.info("Database populated!");
    }

    /**
     * Adds some pets to the database. Feel free to alter this if it doesn't fit your needs.
     */
    public void addPets() {
        Pet dog = new Pet();
        dog.setName("Spot");
        dog.setSpecies(Species.dog);
        dog.setAge(2);
        petRepository.save(dog);

        Pet cat = new Pet();
        cat.setName("Mittens");
        cat.setSpecies(Species.cat);
        cat.setAge(3);
        petRepository.save(cat);

        Pet rabbit = new Pet();
        rabbit.setName("Bun");
        rabbit.setSpecies(Species.rabbit);
        rabbit.setAge(1);
        petRepository.save(rabbit);

        Pet hamster = new Pet();
        hamster.setName("Hammy");
        hamster.setSpecies(Species.hamster);
        hamster.setAge(1);
        petRepository.save(hamster);

        Pet bird = new Pet();
        bird.setName("Tweety");
        bird.setSpecies(Species.bird);
        bird.setAge(1);
        petRepository.save(bird);
    }
}
