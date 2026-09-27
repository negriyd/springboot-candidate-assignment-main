package com.interzero.TestServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies {@code GET /pets/{id}/history/as-of} and {@code GET /owners/{id}/history/as-of}: a resource as it was at a
 * point in time, with related data as of the same moment.
 * <p>
 * Timeline built before each test, with a short pause between steps so every change has its own millisecond:
 * <ol>
 *     <li>{@code beforeCreate}</li>
 *     <li>owner "Jane Doe" and pet "Rex" (age 2, owned by Jane) are created → {@code afterCreate}</li>
 *     <li>the pet's age changes to 3 → {@code afterPetUpdate}</li>
 *     <li>the owner is renamed to "Smith"; the pet itself does not change → {@code afterOwnerRename}</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AsOfTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long ownerId;

    private long petId;

    private Instant beforeCreate;

    private Instant afterCreate;

    private Instant afterPetUpdate;

    private Instant afterOwnerRename;

    @BeforeEach
    void buildTimeline() throws Exception {
        beforeCreate = tick();
        ownerId = createAndGetId("/owners", "{\"nameFirst\": \"Jane\", \"nameLast\": \"Doe\"}");
        petId = createAndGetId("/pets",
                "{\"name\": \"Rex\", \"species\": \"dog\", \"age\": 2, \"ownerId\": %d}".formatted(ownerId));
        afterCreate = tick();
        patchAsWriter("/pets/{id}", petId, "{\"age\": 3}");
        afterPetUpdate = tick();
        patchAsWriter("/owners/{id}", ownerId, "{\"nameLast\": \"Smith\"}");
        afterOwnerRename = tick();
    }

    /**
     * Returns a moment strictly between the previous change and the next one.
     */
    private static Instant tick() throws InterruptedException {
        Thread.sleep(15);
        Instant now = Instant.now();
        Thread.sleep(15);
        return now;
    }

    private long createAndGetId(String path, String body) throws Exception {
        String response = mockMvc.perform(as("writer", post(path)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void patchAsWriter(String path, long id, String body) throws Exception {
        mockMvc.perform(as("writer", patch(path, id))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder as(String user, MockHttpServletRequestBuilder request) {
        return request.with(httpBasic(user, user + "-password"));
    }

    private ResultActions asOf(String resource, long id, Instant time) throws Exception {
        return mockMvc.perform(as("reader", get("/%s/{id}/history/as-of".formatted(resource), id))
                .param("time", time.toString()));
    }

    private JsonNode asOfJson(String resource, long id, Instant time) throws Exception {
        String body = asOf(resource, id, time).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void petAsOfAfterCreation() throws Exception {
        JsonNode entry = asOfJson("pets", petId, afterCreate);

        assertThat(entry.get("changeType").asText()).isEqualTo("CREATED");
        assertThat(entry.get("username").asText()).isEqualTo("writer");
        assertThat(entry.get("version").asLong()).isEqualTo(0);
        assertThat(entry.get("state").get("age").asInt()).isEqualTo(2);
        assertThat(entry.get("state").get("owner").get("nameLast").asText()).isEqualTo("Doe");
    }

    @Test
    void petAsOfAfterUpdate() throws Exception {
        JsonNode entry = asOfJson("pets", petId, afterPetUpdate);

        assertThat(entry.get("changeType").asText()).isEqualTo("UPDATED");
        assertThat(entry.get("version").asLong()).isEqualTo(1);
        assertThat(entry.get("state").get("age").asInt()).isEqualTo(3);
        assertThat(entry.get("state").get("owner").get("nameLast").asText()).isEqualTo("Doe");
    }

    @Test
    void relatedDataIsShownAsOfTheSameMoment() throws Exception {
        JsonNode beforeRename = asOfJson("pets", petId, afterPetUpdate);
        JsonNode afterRename = asOfJson("pets", petId, afterOwnerRename);

        // The pet did not change in between, so both describe the same last pet change...
        assertThat(afterRename.get("revision").asLong()).isEqualTo(beforeRename.get("revision").asLong());
        assertThat(afterRename.get("state").get("age").asInt()).isEqualTo(3);
        // ...but its owner is shown as it was at each moment.
        assertThat(beforeRename.get("state").get("owner").get("nameLast").asText()).isEqualTo("Doe");
        assertThat(afterRename.get("state").get("owner").get("nameLast").asText()).isEqualTo("Smith");
    }

    @Test
    void ownerAsOf() throws Exception {
        assertThat(asOfJson("owners", ownerId, afterPetUpdate).get("state").get("nameLast").asText()).isEqualTo("Doe");
        assertThat(asOfJson("owners", ownerId, afterOwnerRename).get("state").get("nameLast").asText())
                .isEqualTo("Smith");
    }

    @Test
    void futureTimeReturnsCurrentState() throws Exception {
        JsonNode entry = asOfJson("pets", petId, Instant.now().plusSeconds(3600));
        assertThat(entry.get("state").get("age").asInt()).isEqualTo(3);
        assertThat(entry.get("state").get("owner").get("nameLast").asText()).isEqualTo("Smith");
    }

    @Test
    void beforeCreationReturns404() throws Exception {
        asOf("pets", petId, beforeCreate)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("did not exist yet")));
    }

    @Test
    void afterDeletionReturns404ButEarlierStateIsAvailable() throws Exception {
        mockMvc.perform(as("writer", delete("/pets/{id}", petId))).andExpect(status().isNoContent());
        Instant afterDelete = tick();

        asOf("pets", petId, afterDelete)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("had already been deleted")));
        assertThat(asOfJson("pets", petId, afterPetUpdate).get("state").get("age").asInt()).isEqualTo(3);
    }

    @Test
    void unknownPetReturns404() throws Exception {
        asOf("pets", Long.MAX_VALUE, afterCreate)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pet %d not found.".formatted(Long.MAX_VALUE)));
    }

    @Test
    void timeWithOffsetIsAccepted() throws Exception {
        String withOffset = afterCreate.atOffset(ZoneOffset.ofHours(3)).toString().replace("+", "%2B");
        mockMvc.perform(as("reader", get(URI.create(
                        "/pets/%d/history/as-of?time=%s".formatted(petId, withOffset)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.age").value(2));
    }

    @Test
    void missingOrInvalidTimeReturns400() throws Exception {
        mockMvc.perform(as("reader", get("/pets/{id}/history/as-of", petId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as("reader", get("/pets/{id}/history/as-of", petId)).param("time", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("time")));
    }

    @Test
    void writerCannotRead() throws Exception {
        mockMvc.perform(as("writer", get("/pets/{id}/history/as-of", petId)).param("time", afterCreate.toString()))
                .andExpect(status().isForbidden());
    }
}
