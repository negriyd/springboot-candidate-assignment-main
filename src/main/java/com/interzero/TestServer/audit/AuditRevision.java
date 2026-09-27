package com.interzero.TestServer.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * One Envers revision: a transaction that changed at least one audited entity ({@code @Audited}).
 * <p>
 * The history tables ({@code pet_aud}, {@code owner_aud}) store one row per changed entity per revision and refer to
 * this row, which records when the change happened and who made it.
 */
@Getter
@Setter
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision {

    /**
     * The revision number. Increases with every transaction that changes audited data.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;

    /**
     * When the revision was committed, in milliseconds since the epoch.
     */
    @RevisionTimestamp
    private long timestamp;

    /**
     * The user who made the change, or {@link AuditRevisionListener#SYSTEM_USER} for changes made by the application
     * itself, e.g. the startup data.
     */
    @Column(nullable = false, length = 100)
    private String username;
}
