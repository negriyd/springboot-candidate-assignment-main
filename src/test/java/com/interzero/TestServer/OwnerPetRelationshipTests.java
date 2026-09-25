package com.interzero.TestServer;

import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link Pet#setOwner(Owner)} keeps both sides of the owner-pet relationship in sync.
 * Plain unit tests: no Spring context or database needed.
 */
class OwnerPetRelationshipTests {

    @Test
    void assigningOwnerAddsPetToOwner() {
        Owner owner = new Owner();
        Pet pet = new Pet();

        pet.setOwner(owner);

        assertThat(pet.getOwner()).isSameAs(owner);
        assertThat(owner.getPets()).containsExactly(pet);
    }

    @Test
    void changingOwnerMovesPet() {
        Owner previous = new Owner();
        Owner next = new Owner();
        Pet pet = new Pet();
        pet.setOwner(previous);

        pet.setOwner(next);

        assertThat(previous.getPets()).isEmpty();
        assertThat(next.getPets()).containsExactly(pet);
    }

    @Test
    void removingOwnerRemovesPetFromOwner() {
        Owner owner = new Owner();
        Pet pet = new Pet();
        pet.setOwner(owner);

        pet.setOwner(null);

        assertThat(pet.getOwner()).isNull();
        assertThat(owner.getPets()).isEmpty();
    }

    @Test
    void assigningSameOwnerTwiceDoesNotDuplicate() {
        Owner owner = new Owner();
        Pet pet = new Pet();

        pet.setOwner(owner);
        pet.setOwner(owner);

        assertThat(owner.getPets()).containsExactly(pet);
    }

    @Test
    void petsCannotBeChangedThroughOwner() {
        Owner owner = new Owner();
        assertThatThrownBy(() -> owner.getPets().add(new Pet())).isInstanceOf(UnsupportedOperationException.class);
    }
}
