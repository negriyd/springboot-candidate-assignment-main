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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the CRUD endpoints of {@code OwnerController}, the owner-to-pets relationship, validation,
 * 404/409 responses and role rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnerControllerTests {

    private static final String VALID_BODY = """
            {"nameFirst": "John", "nameLast": "Doe", "address": "1 Main Street"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    private Owner existing;

    @BeforeEach
    void createOwner() {
        Owner owner = new Owner();
        owner.setNameFirst("Jane");
        owner.setNameLast("Roe");
        owner.setAddress("5 Side Street");
        existing = ownerRepository.save(owner);
    }

    private Pet addPet(String name, Owner owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(Species.dog);
        pet.setAge(2);
        pet.setOwner(owner);
        return petRepository.save(pet);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("admin", "admin-password"));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return asAdmin(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void getOwnersReturnsPage() throws Exception {
        long total = ownerRepository.count();
        mockMvc.perform(asAdmin(get("/owners").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getOwnersSortsByRequestedField() throws Exception {
        mockMvc.perform(asAdmin(get("/owners").param("sort", "nameLast,desc").param("sort", "id,desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nameLast").value("Smith"));
    }

    @Test
    void getOwnersCannotSortByPets() throws Exception {
        mockMvc.perform(asAdmin(get("/owners").param("sort", "pets")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot sort by 'pets'; allowed: address, id, nameFirst, nameLast."));
    }

    @Test
    void getOwnerReturnsOwnerWithoutPets() throws Exception {
        addPet("Rex", existing);

        mockMvc.perform(asAdmin(get("/owners/{id}", existing.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.nameFirst").value("Jane"))
                .andExpect(jsonPath("$.nameLast").value("Roe"))
                .andExpect(jsonPath("$.address").value("5 Side Street"))
                .andExpect(jsonPath("$.pets").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void getUnknownOwnerReturns404() throws Exception {
        mockMvc.perform(asAdmin(get("/owners/{id}", Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Owner %d not found.".formatted(Long.MAX_VALUE)));
    }

    @Test
    void getOwnerPetsReturnsOnlyTheirPets() throws Exception {
        addPet("Rex", existing);
        addPet("Max", existing);
        addPet("Stray", null);

        mockMvc.perform(asAdmin(get("/owners/{id}/pets", existing.getId()).param("sort", "name")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Max"))
                .andExpect(jsonPath("$.content[0].ownerId").value(existing.getId()))
                .andExpect(jsonPath("$.content[1].name").value("Rex"));
    }

    @Test
    void getPetsOfUnknownOwnerReturns404() throws Exception {
        mockMvc.perform(asAdmin(get("/owners/{id}/pets", Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOwnerReturns201WithLocation() throws Exception {
        String location = mockMvc.perform(json(post("/owners"), VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/owners/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nameFirst").value("John"))
                .andExpect(jsonPath("$.nameLast").value("Doe"))
                .andExpect(jsonPath("$.address").value("1 Main Street"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(asAdmin(get(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameFirst").value("John"));
    }

    @Test
    void createOwnerWithoutAddressIsAllowed() throws Exception {
        mockMvc.perform(json(post("/owners"), """
                        {"nameFirst": "John", "nameLast": "Doe"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address").doesNotExist());
    }

    @Test
    void createInvalidOwnerReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(json(post("/owners"), """
                        {"nameFirst": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("nameFirst:")))
                .andExpect(jsonPath("$.message").value(containsString("nameLast:")));
    }

    @Test
    void updateOwnerReplacesFieldsAndKeepsPets() throws Exception {
        Pet pet = addPet("Rex", existing);

        mockMvc.perform(json(put("/owners/{id}", existing.getId()), VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.nameFirst").value("John"));

        assertThat(petRepository.findById(pet.getId())).get()
                .extracting(Pet::getOwnerId).isEqualTo(existing.getId());
    }

    @Test
    void updateUnknownOwnerReturns404() throws Exception {
        mockMvc.perform(json(put("/owners/{id}", Long.MAX_VALUE), VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchOwnerChangesOnlyGivenFields() throws Exception {
        mockMvc.perform(json(patch("/owners/{id}", existing.getId()), """
                        {"nameLast": "Smith"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId()))
                .andExpect(jsonPath("$.nameFirst").value("Jane"))
                .andExpect(jsonPath("$.nameLast").value("Smith"))
                .andExpect(jsonPath("$.address").value("5 Side Street"));

        assertThat(ownerRepository.findById(existing.getId())).get()
                .extracting(Owner::getNameLast).isEqualTo("Smith");
    }

    @Test
    void patchOwnerWithNullAddressRemovesAddress() throws Exception {
        mockMvc.perform(json(patch("/owners/{id}", existing.getId()), """
                        {"address": null}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.nameFirst").value("Jane"));

        assertThat(ownerRepository.findById(existing.getId())).get()
                .extracting(Owner::getAddress).isNull();
    }

    @Test
    void patchOwnerWithNullRequiredFieldKeepsValue() throws Exception {
        mockMvc.perform(json(patch("/owners/{id}", existing.getId()), """
                        {"nameFirst": null, "address": "9 New Road"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameFirst").value("Jane"))
                .andExpect(jsonPath("$.address").value("9 New Road"));
    }

    @Test
    void patchOwnerKeepsPets() throws Exception {
        Pet pet = addPet("Rex", existing);

        mockMvc.perform(json(patch("/owners/{id}", existing.getId()), """
                        {"nameFirst": "Janet"}
                        """))
                .andExpect(status().isOk());

        assertThat(petRepository.findById(pet.getId())).get()
                .extracting(Pet::getOwnerId).isEqualTo(existing.getId());
    }

    @Test
    void patchOwnerAcceptsMergePatchContentType() throws Exception {
        mockMvc.perform(asAdmin(patch("/owners/{id}", existing.getId()))
                        .contentType("application/merge-patch+json")
                        .content("""
                                {"nameFirst": "Janet"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameFirst").value("Janet"));
    }

    @Test
    void patchInvalidOwnerReturns400() throws Exception {
        mockMvc.perform(json(patch("/owners/{id}", existing.getId()), """
                        {"nameFirst": "  ", "address": "%s"}
                        """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("nameFirst: must not be blank")))
                .andExpect(jsonPath("$.message").value(containsString("address:")));

        assertThat(ownerRepository.findById(existing.getId())).get()
                .extracting(Owner::getNameFirst).isEqualTo("Jane");
    }

    @Test
    void patchUnknownOwnerReturns404() throws Exception {
        mockMvc.perform(json(patch("/owners/{id}", Long.MAX_VALUE), """
                        {"nameFirst": "Janet"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void readerCannotPatchOwner() throws Exception {
        mockMvc.perform(patch("/owners/{id}", existing.getId()).with(httpBasic("reader", "reader-password"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nameFirst\": \"Janet\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOwnerWithoutPetsReturns204() throws Exception {
        mockMvc.perform(asAdmin(delete("/owners/{id}", existing.getId())))
                .andExpect(status().isNoContent());

        assertThat(ownerRepository.findById(existing.getId())).isEmpty();
    }

    @Test
    void deleteOwnerWithPetsReturns409() throws Exception {
        Pet pet = addPet("Rex", existing);

        mockMvc.perform(asAdmin(delete("/owners/{id}", existing.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Owner %d still has pets; reassign or delete them first.".formatted(existing.getId())));

        assertThat(ownerRepository.findById(existing.getId())).isPresent();
        assertThat(petRepository.findById(pet.getId())).get()
                .extracting(Pet::getOwnerId).isEqualTo(existing.getId());
    }

    @Test
    void deleteUnknownOwnerReturns404() throws Exception {
        mockMvc.perform(asAdmin(delete("/owners/{id}", Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void readerCanReadButNotWrite() throws Exception {
        mockMvc.perform(get("/owners/{id}", existing.getId()).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/owners/{id}/pets", existing.getId()).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/owners").with(httpBasic("reader", "reader-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/owners/{id}", existing.getId()).with(httpBasic("reader", "reader-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writerCanWriteButNotRead() throws Exception {
        mockMvc.perform(get("/owners/{id}", existing.getId()).with(httpBasic("writer", "writer-password")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/owners").with(httpBasic("writer", "writer-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }
}
