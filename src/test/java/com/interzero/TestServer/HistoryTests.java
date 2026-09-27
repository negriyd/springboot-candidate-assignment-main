package com.interzero.TestServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interzero.TestServer.entity.Owner;
import com.interzero.TestServer.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the change history (Hibernate Envers) exposed by {@code GET /pets/{id}/history} and
 * {@code GET /owners/{id}/history}: who changed what and when, newest first, including deletions.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HistoryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnerRepository ownerRepository;

    private Owner owner;

    @BeforeEach
    void createOwner() {
        Owner newOwner = new Owner();
        newOwner.setNameFirst("Jane");
        newOwner.setNameLast("Doe");
        owner = ownerRepository.save(newOwner);
    }

    private static MockHttpServletRequestBuilder as(String user, MockHttpServletRequestBuilder request) {
        return request.with(httpBasic(user, user + "-password"));
    }

    private long createPetAsWriter() throws Exception {
        String body = mockMvc.perform(as("writer", post("/pets"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Rex\", \"species\": \"dog\", \"age\": 2}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void patchPet(String user, long id, String body) throws Exception {
        mockMvc.perform(as(user, patch("/pets/{id}", id)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private JsonNode history(String path, Object id) throws Exception {
        String body = mockMvc.perform(as("reader", get(path, id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void historyRecordsWhoChangedWhatNewestFirst() throws Exception {
        long id = createPetAsWriter();
        patchPet("admin", id, "{\"age\": 3}");

        JsonNode entries = history("/pets/{id}/history", id).get("content");

        assertThat(entries).hasSize(2);
        JsonNode update = entries.get(0);
        assertThat(update.get("changeType").asText()).isEqualTo("UPDATED");
        assertThat(update.get("username").asText()).isEqualTo("admin");
        assertThat(update.get("version").asLong()).isEqualTo(1);
        assertThat(update.get("state").get("age").asInt()).isEqualTo(3);
        assertThat(update.get("timestamp").asText()).isNotBlank();

        JsonNode creation = entries.get(1);
        assertThat(creation.get("changeType").asText()).isEqualTo("CREATED");
        assertThat(creation.get("username").asText()).isEqualTo("writer");
        assertThat(creation.get("version").asLong()).isEqualTo(0);
        assertThat(creation.get("state").get("age").asInt()).isEqualTo(2);

        assertThat(update.get("revision").asLong()).isGreaterThan(creation.get("revision").asLong());
    }

    @Test
    void historyShowsOwnerAsOfEachChange() throws Exception {
        long id = createPetAsWriter();
        patchPet("writer", id, "{\"ownerId\": %d}".formatted(owner.getId()));
        mockMvc.perform(as("admin", patch("/owners/{id}", owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nameLast\": \"Smith\"}"))
                .andExpect(status().isOk());
        patchPet("writer", id, "{\"age\": 5}");

        JsonNode entries = history("/pets/{id}/history", id).get("content");

        assertThat(entries).hasSize(3);
        // Newest pet revision: the owner had been renamed by then.
        assertThat(entries.get(0).get("state").get("owner").get("nameLast").asText()).isEqualTo("Smith");
        // The revision that assigned the owner: the owner still had the old name.
        assertThat(entries.get(1).get("state").get("ownerId").asLong()).isEqualTo(owner.getId());
        assertThat(entries.get(1).get("state").get("owner").get("nameLast").asText()).isEqualTo("Doe");
        // Created without an owner.
        assertThat(entries.get(2).get("state").get("owner").isNull()).isTrue();
    }

    @Test
    void deletedPetKeepsHistoryWithLastState() throws Exception {
        long id = createPetAsWriter();
        mockMvc.perform(as("writer", delete("/pets/{id}", id))).andExpect(status().isNoContent());

        mockMvc.perform(as("reader", get("/pets/{id}", id))).andExpect(status().isNotFound());

        JsonNode deletion = history("/pets/{id}/history", id).get("content").get(0);
        assertThat(deletion.get("changeType").asText()).isEqualTo("DELETED");
        assertThat(deletion.get("username").asText()).isEqualTo("writer");
        assertThat(deletion.get("state").get("name").asText()).isEqualTo("Rex");
    }

    @Test
    void historyIsPaged() throws Exception {
        long id = createPetAsWriter();
        patchPet("writer", id, "{\"age\": 3}");
        patchPet("writer", id, "{\"age\": 4}");

        mockMvc.perform(as("reader", get("/pets/{id}/history", id)).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].changeType").value("CREATED"));
    }

    @Test
    void invalidPagingReturns400() throws Exception {
        long id = createPetAsWriter();
        mockMvc.perform(as("reader", get("/pets/{id}/history", id)).param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as("reader", get("/pets/{id}/history", id)).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyOfUnknownPetReturns404() throws Exception {
        mockMvc.perform(as("reader", get("/pets/{id}/history", Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pet %d not found.".formatted(Long.MAX_VALUE)));
    }

    @Test
    void ownerHistoryRecordsOwnFieldsOnly() throws Exception {
        mockMvc.perform(as("writer", patch("/owners/{id}", owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"address\": \"1 Main Street\"}"))
                .andExpect(status().isOk());
        // Assigning a pet to the owner changes the pet, not the owner, so it adds no owner revision.
        long petId = createPetAsWriter();
        patchPet("writer", petId, "{\"ownerId\": %d}".formatted(owner.getId()));

        JsonNode entries = history("/owners/{id}/history", owner.getId()).get("content");

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("changeType").asText()).isEqualTo("UPDATED");
        assertThat(entries.get(0).get("state").get("address").asText()).isEqualTo("1 Main Street");
        assertThat(entries.get(1).get("changeType").asText()).isEqualTo("CREATED");
        // Created directly through the repository in this test, outside any request: recorded as "system".
        assertThat(entries.get(1).get("username").asText()).isEqualTo("system");
    }

    @Test
    void historyIsReadOnlyForWriters() throws Exception {
        long id = createPetAsWriter();
        mockMvc.perform(as("writer", get("/pets/{id}/history", id)))
                .andExpect(status().isForbidden());
    }
}
