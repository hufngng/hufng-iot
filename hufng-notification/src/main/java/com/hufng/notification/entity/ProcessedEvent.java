package com.hufng.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Write-time exact dedupe for the alert side-effect (Telegram/Discord can't be
 * sent "twice, corrected later" the way a ClickHouse telemetry row can).
 * PK is (event_id, consumer) — event_id identifies the event itself, not a
 * business key like deviceId, because one device can legitimately raise many
 * different alert events.
 *
 * Implements Persistable so Spring Data always calls entityManager.persist()
 * (INSERT) instead of merge() (SELECT-then-INSERT-or-UPDATE): this entity has
 * no @GeneratedValue id, so Spring Data's default isNew() check (id == null)
 * would otherwise always see a non-null id and silently UPDATE a "duplicate"
 * row instead of throwing the PK violation this class's whole purpose depends on.
 */
@Entity
@Table(name = "processed_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent implements Persistable<ProcessedEvent.Id> {

    @EmbeddedId
    private Id id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEvent(String eventId, String consumer, Instant processedAt) {
        this.id = new Id(eventId, consumer);
        this.processedAt = processedAt;
    }

    @Transient
    @Override
    public boolean isNew() {
        return true;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "event_id", length = 64)
        private String eventId;

        @Column(name = "consumer", length = 64)
        private String consumer;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(eventId, id.eventId) && Objects.equals(consumer, id.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumer);
        }
    }
}
