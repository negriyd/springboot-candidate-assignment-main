package com.interzero.TestServer.controller;

import com.interzero.TestServer.configuration.CanRead;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.repository.PetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The REST controller for all things related to {@link Pet}s.
 */
@Slf4j
@RestController
@RequestMapping("pets")
public class PetController {

    /**
     * The pet repository. This is used to interact with the database.
     */
    private final PetRepository petRepository;

    public PetController(@NonNull PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    /**
     * Gets all of the pets in the database.
     * @return All of the pets in the database.
     */
    @GetMapping
    @CanRead
    public List<Pet> getPets() {
        log.info("PetController.getPets() called");
        return petRepository.findAll();
    }
}
