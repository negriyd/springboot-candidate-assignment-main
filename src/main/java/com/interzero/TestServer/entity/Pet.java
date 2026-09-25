package com.interzero.TestServer.entity;

import com.interzero.TestServer.enums.Species;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * The pet entity. This is a simple entity that has a name, species, and age.
 * It also has an owner ID, which is the ID of the owner that owns this pet.
 * A pet can only have one owner.
 */
@Getter
@Setter
@Entity
public class Pet {

    /**
     * The ID of the pet. This is the primary key of the pet table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the pet.
     */
    @Column
    private String name;

    /**
     * The species of the pet.
     */
    @Column
    private Species species;

    /**
     * The age of the pet.
     */
    @Column
    private Integer age;

    /**
     * The ID of the owner that owns this pet.
     */
    @Column
    private Long ownerId;
}
