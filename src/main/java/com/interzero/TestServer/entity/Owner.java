package com.interzero.TestServer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The owner entity. This is a simple entity that has a first name, last name, and address.
 * An owner can have many {@link Pet}s; the relationship is stored on the pet side ({@code pet.owner_id}).
 */
@Getter
@Setter
@Entity
public class Owner {

    /**
     * The ID of the owner. This is the primary key of the owner table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Incremented on every update. Used for optimistic locking and exposed to clients as the {@code ETag} header.
     */
    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    /**
     * The first name of the owner.
     */
    @Column(nullable = false, length = 100)
    private String nameFirst;

    /**
     * The last name of the owner.
     */
    @Column(nullable = false, length = 100)
    private String nameLast;

    /**
     * The address of the owner.
     */
    @Column(length = 255)
    private String address;

    /**
     * The pets that this owner owns.
     * Changed only through {@link Pet#setOwner(Owner)}, which keeps both sides of the relationship in sync.
     */
    @OneToMany(mappedBy = "owner")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<Pet> pets = new ArrayList<>();

    /**
     * The pets that this owner owns. To change them, assign the owner on the pet with {@link Pet#setOwner(Owner)}.
     *
     * @return A read-only view of the pets.
     */
    public List<Pet> getPets() {
        return Collections.unmodifiableList(pets);
    }

    /**
     * Adds a pet to this owner's collection. Called only by {@link Pet#setOwner(Owner)}.
     */
    void attach(Pet pet) {
        pets.add(pet);
    }

    /**
     * Removes a pet from this owner's collection. Called only by {@link Pet#setOwner(Owner)}.
     */
    void detach(Pet pet) {
        pets.remove(pet);
    }
}
