package com.interzero.TestServer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
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
     * The pets that this owner owns. Not serialized; use {@code GET /owners/{id}/pets}, which is paged.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "owner")
    private List<Pet> pets = new ArrayList<>();
}
