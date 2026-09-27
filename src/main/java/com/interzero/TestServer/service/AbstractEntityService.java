package com.interzero.TestServer.service;

import com.interzero.TestServer.dto.HistoryEntry;
import com.interzero.TestServer.entity.VersionedEntity;
import com.interzero.TestServer.error.ResourceNotFoundException;
import com.interzero.TestServer.error.VersionMismatchException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Function;

/**
 * Behavior shared by the services of all versioned, audited entities: reading one entity, optimistic locking for
 * changes ({@code If-Match}), deleting, and the change history.
 * <p>
 * Subclasses add what differs per entity: list queries with their own filters, and creating and updating from their
 * own request types. Every method runs in a transaction, so a lookup or check and the write that follows it see a
 * consistent state.
 *
 * @param <E> The entity type.
 * @param <R> The API response type.
 */
@Transactional
public abstract class AbstractEntityService<E extends VersionedEntity, R> {

    private final JpaRepository<E, Long> repository;

    private final Class<E> entityType;

    private final Function<E, R> toResponse;

    /**
     * Used to read the change history from the Envers tables.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param repository The repository of the entity.
     * @param entityType The entity class; its simple name is used in error messages, e.g. "Pet 5 not found."
     * @param toResponse Maps an entity to its API response. Called inside the transaction, so it can follow relations.
     */
    protected AbstractEntityService(JpaRepository<E, Long> repository, Class<E> entityType,
                                    Function<E, R> toResponse) {
        this.repository = repository;
        this.entityType = entityType;
        this.toResponse = toResponse;
    }

    /**
     * Gets a single entity.
     *
     * @param id The ID.
     * @return The entity.
     * @throws ResourceNotFoundException If no entity with this ID exists.
     */
    @Transactional(readOnly = true)
    public R get(Long id) {
        return toResponse.apply(find(id));
    }

    /**
     * Deletes an entity. Runs {@link #beforeDelete} first, so subclasses can refuse the deletion.
     *
     * @param id              The ID.
     * @param expectedVersion The version the client last read ({@code If-Match}), or {@code null} to skip
     *                        the check.
     * @throws ResourceNotFoundException If no entity with this ID exists.
     * @throws VersionMismatchException  If {@code expectedVersion} does not match the current version.
     */
    public void delete(Long id, Long expectedVersion) {
        E entity = findForUpdate(id, expectedVersion);
        beforeDelete(entity);
        repository.delete(entity);
    }

    /**
     * Gets one page of the change history of an entity, newest change first. Works for deleted entities too.
     *
     * @param id       The ID.
     * @param pageable The page to return; any sort is ignored.
     * @return The requested page of history entries.
     * @throws ResourceNotFoundException If no entity with this ID has ever existed.
     */
    @Transactional(readOnly = true)
    public Page<HistoryEntry<R>> history(Long id, Pageable pageable) {
        Page<HistoryEntry<R>> history = AuditHistory.load(entityManager, entityType, id, pageable, toResponse);
        if (history.getTotalElements() == 0) {
            throw notFound(id);
        }
        return history;
    }

    /**
     * Gets an entity as it was at a point in time, including its related data as of that moment.
     *
     * @param id   The ID.
     * @param time The point in time.
     * @return The entity at that time, with the details of its last change before that time.
     * @throws ResourceNotFoundException If the entity never existed, did not exist yet at that time, or had already
     *                                   been deleted.
     */
    @Transactional(readOnly = true)
    public HistoryEntry<R> asOf(Long id, Instant time) {
        return AuditHistory.asOf(entityManager, entityType, id, time, toResponse);
    }

    /**
     * Called by {@link #delete} after the lookup and version check, before the entity is deleted. Does nothing by
     * default; override to refuse the deletion by throwing an exception.
     *
     * @param entity The entity about to be deleted.
     */
    protected void beforeDelete(E entity) {
    }

    /**
     * Finds an entity.
     *
     * @param id The ID.
     * @return The entity.
     * @throws ResourceNotFoundException If no entity with this ID exists.
     */
    protected E find(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    /**
     * Finds an entity to change, checking that it still has the version the client expects.
     *
     * @param id              The ID.
     * @param expectedVersion The version the client last read, or {@code null} to skip the check.
     * @return The entity.
     * @throws ResourceNotFoundException If no entity with this ID exists.
     * @throws VersionMismatchException  If the versions differ.
     */
    protected E findForUpdate(Long id, Long expectedVersion) {
        E entity = find(id);
        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            throw new VersionMismatchException(
                    "%s %d has been modified since version %d (current version: %d); fetch it again and retry."
                            .formatted(entityName(), id, expectedVersion, entity.getVersion()));
        }
        return entity;
    }

    /**
     * Saves a new or changed entity and maps it to its response. Flushes first, so the response carries the new
     * version (the {@code ETag}) rather than the version from before the change.
     *
     * @param entity The entity.
     * @return The response.
     */
    protected R saveAndMap(E entity) {
        return toResponse.apply(repository.saveAndFlush(entity));
    }

    /**
     * Maps an entity to its API response.
     *
     * @param entity The entity.
     * @return The response.
     */
    protected R toResponse(E entity) {
        return toResponse.apply(entity);
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("%s %d not found.".formatted(entityName(), id));
    }

    private String entityName() {
        return entityType.getSimpleName();
    }
}
