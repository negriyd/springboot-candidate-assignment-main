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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies optimistic locking: single-resource responses carry an {@code ETag} with the entity version, and
 * {@code PUT}/{@code PATCH}/{@code DELETE} with an outdated {@code If-Match} are rejected with 412 instead of
 * silently overwriting someone else's change.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OptimisticLockingTests {

    private static final String PET_BODY = """
            {"name": "Rex", "species": "dog", "age": 4}
            """;

    private static final String OWNER_BODY = """
            {"nameFirst": "Jane", "nameLast": "Doe"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private Pet pet;

    private Owner owner;

    @BeforeEach
    void createData() {
        Pet newPet = new Pet();
        newPet.setName("Felix");
        newPet.setSpecies(Species.cat);
        newPet.setAge(5);
        pet = petRepository.save(newPet);

        Owner newOwner = new Owner();
        newOwner.setNameFirst("John");
        newOwner.setNameLast("Roe");
        owner = ownerRepository.save(newOwner);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("admin", "admin-password"));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return asAdmin(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void responsesCarryETagWithVersion() throws Exception {
        mockMvc.perform(asAdmin(get("/pets/{id}", pet.getId())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.version").doesNotExist());

        mockMvc.perform(json(post("/pets"), PET_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""));
    }

    @Test
    void updateIncrementsETag() throws Exception {
        mockMvc.perform(json(put("/pets/{id}", pet.getId()), PET_BODY).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""));

        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}").header(HttpHeaders.IF_MATCH, "\"1\""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""));
    }

    @Test
    void staleIfMatchOnPutReturns412AndKeepsData() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"name\": \"First\"}"))
                .andExpect(status().isOk());

        // A second client still holds version 0 and tries to overwrite the first client's change.
        mockMvc.perform(json(put("/pets/{id}", pet.getId()), PET_BODY).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412))
                .andExpect(jsonPath("$.message").value(containsString("has been modified since version 0")));

        assertThat(petRepository.findById(pet.getId())).get().extracting(Pet::getName).isEqualTo("First");
    }

    @Test
    void staleIfMatchOnPatchReturns412() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}"))
                .andExpect(status().isOk());

        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 7}").header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void staleIfMatchOnDeleteReturns412() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}"))
                .andExpect(status().isOk());

        mockMvc.perform(asAdmin(delete("/pets/{id}", pet.getId())).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());
        assertThat(petRepository.findById(pet.getId())).isPresent();

        mockMvc.perform(asAdmin(delete("/pets/{id}", pet.getId())).header(HttpHeaders.IF_MATCH, "\"1\""))
                .andExpect(status().isNoContent());
    }

    @Test
    void ownerEndpointsUseETagsToo() throws Exception {
        mockMvc.perform(asAdmin(get("/owners/{id}", owner.getId())))
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""));

        mockMvc.perform(json(put("/owners/{id}", owner.getId()), OWNER_BODY).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""));

        mockMvc.perform(json(patch("/owners/{id}", owner.getId()), "{\"address\": \"x\"}")
                        .header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());

        mockMvc.perform(asAdmin(delete("/owners/{id}", owner.getId())).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void withoutIfMatchChangesAreUnconditional() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}"))
                .andExpect(status().isOk());
        mockMvc.perform(json(put("/pets/{id}", pet.getId()), PET_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void wildcardIfMatchMatchesAnyVersion() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}"))
                .andExpect(status().isOk());
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 7}").header(HttpHeaders.IF_MATCH, "*"))
                .andExpect(status().isOk());
    }

    @Test
    void malformedIfMatchReturns400() throws Exception {
        mockMvc.perform(json(patch("/pets/{id}", pet.getId()), "{\"age\": 6}").header(HttpHeaders.IF_MATCH, "\"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid If-Match header")));
    }

    @Test
    void concurrentTransactionsCannotOverwriteEachOther() {
        // Two transactions read version 0; the first one commits, so the second one must fail.
        Pet first = petRepository.findById(pet.getId()).orElseThrow();
        Pet second = petRepository.findById(pet.getId()).orElseThrow();

        first.setName("First");
        petRepository.saveAndFlush(first);

        second.setName("Second");
        assertThatThrownBy(() -> petRepository.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(petRepository.findById(pet.getId())).get().extracting(Pet::getName).isEqualTo("First");
    }
}
