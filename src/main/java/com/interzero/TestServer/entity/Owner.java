package com.interzero.TestServer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * The owner entity. This is a simple entity that has a first name, last name, and address.
 * It also has a pet ID, which is the ID of the pet that this owner owns.
 * An owner can only have multiple pets.
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
     * The first name of the owner.
     */
    @Column
    private String nameFirst;

    /**
     * The last name of the owner.
     */
    @Column
    private String nameLast;

    /**
     * The address of the owner.
     */
    @Column
    private String address;

    /**
     * The IDs of the pets that this owner owns.
     * seems like it can only hold one ID right now...
     */
    @Column
    private Integer petId;
}
