package com.interzero.TestServer;

import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.entity.Pet;
import com.interzero.TestServer.enums.Species;
import com.interzero.TestServer.repository.OwnerRepository;
import com.interzero.TestServer.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the optional filters on list endpoints: {@code GET /pets}, {@code GET /owners/{id}/pets}
 * and {@code GET /owners}.
 * <p>
 * The database is shared with other tests, so every test works with names containing a unique {@link #token}
 * and filters by it, which keeps results independent of other data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FilteringTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private String token;

    private Owner alice;

    private Owner bob;

    @BeforeEach
    void createData() {
        token = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        alice = addOwner("Alice", "Smith" + token, "12 Oak Street " + token);
        bob = addOwner("Bob", "Jones" + token, "34 Elm Street " + token);

        addPet("Rex " + token, Species.dog, 2, alice);
        addPet("Max " + token, Species.dog, 7, bob);
        addPet("Tom " + token, Species.cat, 4, alice);
        addPet("Bun " + token, Species.rabbit, 1, null);
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

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("admin", "admin-password"));
    }

    @Test
    void petsFilteredByNameIgnoringCase() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        mockMvc.perform(asAdmin(get("/pets").param("name", "rex " + token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rex " + token));
    }

    @Test
    void petsFilteredBySpecies() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("species", "dog")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Rex " + token, "Max " + token)));
    }

    @Test
    void petsFilteredByMultipleSpecies() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("species", "cat", "rabbit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Tom " + token, "Bun " + token)));

        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("species", "cat,rabbit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void petsFilteredByAgeRange() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("minAge", "2").param("maxAge", "4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Rex " + token, "Tom " + token)));
    }

    @Test
    void petsFilteredByOwner() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("ownerId", alice.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("Rex " + token, "Tom " + token)));
    }

    @Test
    void petsWithoutOwner() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("hasOwner", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bun " + token))
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist());
    }

    @Test
    void petsWithOwner() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("hasOwner", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name")
                        .value(containsInAnyOrder("Rex " + token, "Max " + token, "Tom " + token)));
    }

    @Test
    void ownerIdWithHasOwnerFalseMatchesNothing() throws Exception {
        mockMvc.perform(asAdmin(get("/pets")
                        .param("ownerId", alice.getId().toString())
                        .param("hasOwner", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidHasOwnerReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("hasOwner", "maybe")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void petsFiltersAreCombined() throws Exception {
        mockMvc.perform(asAdmin(get("/pets")
                        .param("ownerId", alice.getId().toString())
                        .param("species", "dog")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rex " + token));
    }

    @Test
    void blankFilterIsIgnored() throws Exception {
        long total = petRepository.count();
        mockMvc.perform(asAdmin(get("/pets").param("name", " ").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(total));
    }

    @Test
    void likeWildcardsMatchLiterally() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", "%")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(asAdmin(get("/pets").param("name", "_")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersWorkWithPagingAndSorting() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("name", token).param("sort", "age,desc").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Max " + token))
                .andExpect(jsonPath("$.content[1].name").value("Tom " + token));
    }

    @Test
    void unknownSpeciesReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("species", "fish")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("species: invalid value 'fish'"));
    }

    @Test
    void negativeAgeReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("minAge", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("minAge:")));
    }

    @Test
    void invertedAgeRangeReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("minAge", "5").param("maxAge", "2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("minAge must not be greater than maxAge")));
    }

    @Test
    void invalidOwnerIdReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("ownerId", "abc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerPetsFiltered() throws Exception {
        mockMvc.perform(asAdmin(get("/owners/{id}/pets", alice.getId()).param("species", "cat")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tom " + token));
    }

    @Test
    void ownerPetsInvalidFilterReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/owners/{id}/pets", alice.getId()).param("maxAge", "-3")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownersFilteredByLastName() throws Exception {
        mockMvc.perform(asAdmin(get("/owners").param("nameLast", "smith" + token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alice.getId()));
    }

    @Test
    void ownersFilteredByFirstNameAndAddress() throws Exception {
        mockMvc.perform(asAdmin(get("/owners").param("nameFirst", "bo").param("address", "elm street " + token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bob.getId()));

        mockMvc.perform(asAdmin(get("/owners").param("nameFirst", "alice").param("address", "elm street " + token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void ownersTooLongFilterReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/owners").param("nameLast", "x".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("nameLast:")));
    }

    @Test
    void readerCanFilterButWriterCannot() throws Exception {
        mockMvc.perform(get("/pets").param("name", token).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/owners").param("nameLast", token).with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden());
    }
}
