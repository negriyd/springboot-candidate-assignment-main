package com.interzero.TestServer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.interzero.TestServer.enums.Species;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
     * Incremented on every update. Used for optimistic locking and exposed to clients as the {@code ETag} header.
     */
    @JsonIgnore
    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    /**
     * The name of the pet.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * The species of the pet. Stored by name, not by ordinal, so that reordering or adding enum values does not
     * change the meaning of existing rows.
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Species species;

    /**
     * The age of the pet.
     */
    @Column(nullable = false)
    private Integer age;

    /**
     * The owner of this pet, or {@code null} if the pet has no owner.
     * Not serialized; clients see only {@link #getOwnerId()}.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Setter(AccessLevel.NONE)
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

    /**
     * Assigns this pet to an owner, or removes its owner. Keeps both sides of the relationship in sync: the pet is
     * removed from the previous owner's {@link Owner#getPets() pets} and added to the new owner's.
     *
     * @param newOwner The new owner, or {@code null} to remove the owner.
     */
    public void setOwner(Owner newOwner) {
        if (owner == newOwner) {
            return;
        }
        if (owner != null) {
            owner.detach(this);
        }
        owner = newOwner;
        if (newOwner != null) {
            newOwner.attach(this);
        }
    }
}
