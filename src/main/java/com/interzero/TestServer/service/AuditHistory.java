package com.interzero.TestServer.service;

import com.interzero.TestServer.audit.AuditRevision;
import com.interzero.TestServer.dto.HistoryEntry;
import com.interzero.TestServer.dto.HistoryEntry.ChangeType;
import com.interzero.TestServer.entity.VersionedEntity;
import com.interzero.TestServer.error.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Reads the change history of an audited entity from the Envers tables, newest change first.
 */
final class AuditHistory {

    private AuditHistory() {
    }

    /**
     * Gets one page of the history of an entity.
     *
     * @param entityManager The entity manager of the current transaction.
     * @param type          The audited entity type.
     * @param id            The entity ID. The entity may have been deleted; its history is still returned.
     * @param pageable      The page to return; any sort is ignored, the history is always newest first.
     * @param mapper        Maps a historical entity to its API representation. Called inside the transaction, so it
     *                      can follow relations, which Envers resolves as of the same revision.
     * @param <E>           The entity type.
     * @param <R>           The API representation.
     * @return The page of history entries; empty if the entity never existed.
     */
    static <E extends VersionedEntity, R> Page<HistoryEntry<R>> load(EntityManager entityManager, Class<E> type,
                                                                     Long id, Pageable pageable,
                                                                     Function<E, R> mapper) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        long total = ((Number) revisionsOf(reader, type, id)
                .addProjection(AuditEntity.revisionNumber().count())
                .getSingleResult()).longValue();
        if (total == 0) {
            return Page.empty(pageable);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = revisionsOf(reader, type, id)
                .addOrder(AuditEntity.revisionNumber().desc())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<HistoryEntry<R>> entries = rows.stream().map(row -> {
            @SuppressWarnings("unchecked")
            E entity = (E) row[0];
            AuditRevision revision = (AuditRevision) row[1];
            return new HistoryEntry<>(
                    revision.getId(),
                    Instant.ofEpochMilli(revision.getTimestamp()),
                    revision.getUsername(),
                    changeType((RevisionType) row[2]),
                    entity.getVersion(),
                    mapper.apply(entity));
        }).toList();
        return new PageImpl<>(entries, pageable, total);
    }

    /**
     * Gets an entity as it was at a point in time.
     * <p>
     * The state is read at the global revision in effect at that time, so relations are also shown as they were then:
     * if a pet did not change but its owner was renamed before {@code time}, the result has the new owner name. The
     * revision fields of the result describe the entity's own last change at or before {@code time}.
     *
     * @param entityManager The entity manager of the current transaction.
     * @param type          The audited entity type; its simple name is used in error messages.
     * @param id            The entity ID.
     * @param time          The point in time.
     * @param mapper        Maps a historical entity to its API representation. Called inside the transaction.
     * @param <E>           The entity type.
     * @param <R>           The API representation.
     * @return The entity at that time, with its last change before that time.
     * @throws ResourceNotFoundException If the entity never existed, did not exist yet at that time, or had already
     *                                   been deleted.
     */
    static <E extends VersionedEntity, R> HistoryEntry<R> asOf(EntityManager entityManager, Class<E> type, Long id,
                                                               Instant time, Function<E, R> mapper) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        String name = type.getSimpleName();

        @SuppressWarnings("unchecked")
        List<Object[]> lastChange = revisionsOf(reader, type, id)
                .add(AuditEntity.revisionProperty("timestamp").le(time.toEpochMilli()))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(1)
                .getResultList();

        if (lastChange.isEmpty()) {
            boolean everExisted = !revisionsOf(reader, type, id).setMaxResults(1).getResultList().isEmpty();
            throw new ResourceNotFoundException(everExisted
                    ? "%s %d did not exist yet at %s.".formatted(name, id, time)
                    : "%s %d not found.".formatted(name, id));
        }

        AuditRevision revision = (AuditRevision) lastChange.get(0)[1];
        RevisionType revisionType = (RevisionType) lastChange.get(0)[2];
        if (revisionType == RevisionType.DEL) {
            throw new ResourceNotFoundException("%s %d had already been deleted at %s (deleted at %s)."
                    .formatted(name, id, time, Instant.ofEpochMilli(revision.getTimestamp())));
        }

        // Read the entity at the global revision in effect at that time, not at its own last revision, so that
        // relations are resolved as of the same moment.
        Number revisionAtTime = reader.getRevisionNumberForDate(Date.from(time));
        E entity = reader.find(type, id, revisionAtTime);

        return new HistoryEntry<>(
                revision.getId(),
                Instant.ofEpochMilli(revision.getTimestamp()),
                revision.getUsername(),
                changeType(revisionType),
                entity.getVersion(),
                mapper.apply(entity));
    }

    /**
     * All revisions of one entity, including the one that deleted it. Each result row is
     * {@code [entity, AuditRevision, RevisionType]}.
     */
    private static AuditQuery revisionsOf(AuditReader reader, Class<?> type, Long id) {
        return reader.createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.id().eq(id));
    }

    private static ChangeType changeType(RevisionType revisionType) {
        return switch (revisionType) {
            case ADD -> ChangeType.CREATED;
            case MOD -> ChangeType.UPDATED;
            case DEL -> ChangeType.DELETED;
        };
    }
}
