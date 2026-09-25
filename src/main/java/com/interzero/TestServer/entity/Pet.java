package com.interzero.TestServer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.interzero.TestServer.enums.Species;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * The pet entity. This is a simple entity that has a name, species, and age.
 * A pet belongs to at most one {@link Owner}; an owner can have many pets.
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
     * The owner of this pet, or {@code null} if the pet has no owner.
     * Not serialized; clients see only {@link #getOwnerId()}.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    /**
     * The ID of the owner that owns this pet. Does not load the owner.
     *
     * @return The owner ID, or {@code null} if the pet has no owner.
     */
    @JsonProperty("ownerId")
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}
