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
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the CRUD endpoints of {@code PetController}, including validation, 404s and role rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PetControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Owner owner;

    private Pet existing;

    @BeforeEach
    void createOwnerAndPet() {
        Owner newOwner = new Owner();
        newOwner.setNameFirst("Jane");
        newOwner.setNameLast("Doe");
        owner = ownerRepository.save(newOwner);

        Pet pet = new Pet();
        pet.setName("Felix");
        pet.setSpecies(Species.cat);
        pet.setAge(5);
        existing = petRepository.save(pet);
    }

    private String validBody() {
        return """
                {"name": "Rex", "species": "dog", "age": 4, "ownerId": %d}
                """.formatted(owner.getId());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("admin", "admin-password"));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return asAdmin(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void getPetsReturnsFirstPageByDefault() throws Exception {
        long total = petRepository.count();
        mockMvc.perform(asAdmin(get("/pets")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((total + 19) / 20))
                .andExpect(jsonPath("$.content.length()").value(Math.min(total, 20)));
    }

    @Test
    void getPetsReturnsRequestedPage() throws Exception {
        long total = petRepository.count();
        mockMvc.perform(asAdmin(get("/pets").param("page", "1").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((total + 1) / 2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getPetsSortsByRequestedField() throws Exception {
        String firstName = petRepository.findAll(Sort.by(Sort.Direction.DESC, "name", "id")).get(0).getName();
        mockMvc.perform(asAdmin(get("/pets").param("sort", "name,desc").param("sort", "id,desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(firstName));
    }

    @Test
    void getPetsSortsByOwnerId() throws Exception {
        existing.setOwner(owner);
        petRepository.save(existing);

        mockMvc.perform(asAdmin(get("/pets").param("sort", "ownerId,desc").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownerId").value(owner.getId()));
    }

    @Test
    void getPetsCapsPageSize() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("size", "1000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void getPetsWithUnknownSortPropertyReturns400() throws Exception {
        mockMvc.perform(asAdmin(get("/pets").param("sort", "unknown")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot sort by 'unknown'; allowed: age, id, name, ownerId, species."));
    }

    @Test
    void speciesIsStoredByName() {
        String stored = jdbcTemplate.queryForObject(
                "SELECT species FROM pet WHERE id = ?", String.class, existing.getId());
        assertThat(stored).isEqualTo("cat");
    }

    @Test
    void getPetReturnsPet() throws Exception {
        mockMvc.perform(asAdmin(get("/pets/{id}", existing.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.name").value("Felix"))
                .andExpect(jsonPath("$.species").value("cat"));
    }

    @Test
    void petResponseIncludesOwnerSummary() throws Exception {
        existing.setOwner(owner);
        petRepository.save(existing);

        mockMvc.perform(asAdmin(get("/pets/{id}", existing.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.owner.id").value(owner.getId()))
                .andExpect(jsonPath("$.owner.nameFirst").value("Jane"))
                .andExpect(jsonPath("$.owner.nameLast").value("Doe"))
                .andExpect(jsonPath("$.owner.address").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void petListIncludesOwnerSummaries() throws Exception {
        existing.setOwner(owner);
        petRepository.save(existing);

        mockMvc.perform(asAdmin(get("/pets").param("ownerId", owner.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner.nameLast").value("Doe"));
    }

    @Test
    void petWithoutOwnerHasNullOwner() throws Exception {
        mockMvc.perform(asAdmin(get("/pets/{id}", existing.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(nullValue()));
    }

    @Test
    void getUnknownPetReturns404() throws Exception {
        mockMvc.perform(asAdmin(get("/pets/{id}", Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pet %d not found.".formatted(Long.MAX_VALUE)));
    }

    @Test
    void createPetReturns201WithLocation() throws Exception {
        String location = mockMvc.perform(json(post("/pets"), validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/pets/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.species").value("dog"))
                .andExpect(jsonPath("$.age").value(4))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(asAdmin(get(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rex"));
    }

    @Test
    void createPetIgnoresIdInBody() throws Exception {
        mockMvc.perform(json(post("/pets"), """
                        {"id": %d, "name": "Rex", "species": "dog", "age": 4}
                        """.formatted(existing.getId())))
                .andExpect(status().isCreated());

        assertThat(petRepository.findById(existing.getId())).get()
                .extracting(Pet::getName).isEqualTo("Felix");
    }

    @Test
    void createInvalidPetReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(json(post("/pets"), """
                        {"name": " ", "age": -1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name:")))
                .andExpect(jsonPath("$.message").value(containsString("species:")))
                .andExpect(jsonPath("$.message").value(containsString("age:")));
    }

    @Test
    void createPetWithUnknownOwnerReturns400() throws Exception {
        mockMvc.perform(json(post("/pets"), """
                        {"name": "Rex", "species": "dog", "age": 4, "ownerId": %d}
                        """.formatted(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Owner %d not found.".formatted(Long.MAX_VALUE)));
    }

    @Test
    void patchPetAssignsOwner() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", existing.getId()), """
                        {"ownerId": %d}
                        """.formatted(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.name").value("Felix"));
    }

    @Test
    void createPetWithUnknownSpeciesReturns400() throws Exception {
        mockMvc.perform(json(post("/pets"), """
                        {"name": "Nemo", "species": "fish", "age": 1}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePetReplacesFields() throws Exception {
        mockMvc.perform(json(put("/pets/{id}", existing.getId()), validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.species").value("dog"));

        assertThat(petRepository.findById(existing.getId())).get()
                .extracting(Pet::getName).isEqualTo("Rex");
    }

    @Test
    void updateUnknownPetReturns404() throws Exception {
        mockMvc.perform(json(put("/pets/{id}", Long.MAX_VALUE), validBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchPetChangesOnlyGivenFields() throws Exception {
        existing.setOwner(owner);
        petRepository.save(existing);

        mockMvc.perform(json(patch("/pets/{id}", existing.getId()), """
                        {"age": 6}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.name").value("Felix"))
                .andExpect(jsonPath("$.species").value("cat"))
                .andExpect(jsonPath("$.age").value(6))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()));

        assertThat(petRepository.findById(existing.getId())).get()
                .extracting(Pet::getAge).isEqualTo(6);
    }

    @Test
    void patchPetWithNullOwnerRemovesOwner() throws Exception {
        existing.setOwner(owner);
        petRepository.save(existing);

        mockMvc.perform(json(patch("/pets/{id}", existing.getId()), """
                        {"ownerId": null}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.name").value("Felix"));

        assertThat(petRepository.findById(existing.getId())).get()
                .extracting(Pet::getOwnerId).isNull();
    }

    @Test
    void patchPetWithNullRequiredFieldKeepsValue() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", existing.getId()), """
                        {"name": null, "species": "dog"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Felix"))
                .andExpect(jsonPath("$.species").value("dog"));
    }

    @Test
    void patchPetAcceptsMergePatchContentType() throws Exception {
        mockMvc.perform(asAdmin(patch("/pets/{id}", existing.getId()))
                        .contentType("application/merge-patch+json")
                        .content("""
                                {"name": "Tom"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tom"));
    }

    @Test
    void patchInvalidPetReturns400() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", existing.getId()), """
                        {"name": "  ", "age": -1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name: must not be blank")))
                .andExpect(jsonPath("$.message").value(containsString("age:")));

        assertThat(petRepository.findById(existing.getId())).get()
                .extracting(Pet::getName).isEqualTo("Felix");
    }

    @Test
    void patchUnknownPetReturns404() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", Long.MAX_VALUE), """
                        {"age": 6}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void readerCannotPatch() throws Exception {
        mockMvc.perform(patch("/pets/{id}", existing.getId()).with(httpBasic("reader", "reader-password"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"age\": 6}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePetReturns204() throws Exception {
        mockMvc.perform(asAdmin(delete("/pets/{id}", existing.getId())))
                .andExpect(status().isNoContent());

        assertThat(petRepository.findById(existing.getId())).isEmpty();
    }

    @Test
    void deleteUnknownPetReturns404() throws Exception {
        mockMvc.perform(asAdmin(delete("/pets/{id}", Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void readerCanReadButNotWrite() throws Exception {
        mockMvc.perform(get("/pets/{id}", existing.getId()).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/pets").with(httpBasic("reader", "reader-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/pets/{id}", existing.getId()).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writerCanWriteButNotRead() throws Exception {
        mockMvc.perform(get("/pets/{id}", existing.getId()).with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pets").with(httpBasic("writer", "writer-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated());
    }
}
