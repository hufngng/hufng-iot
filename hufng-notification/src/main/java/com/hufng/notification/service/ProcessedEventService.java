package com.hufng.notification.service;

import com.hufng.notification.entity.ProcessedEvent;
import com.hufng.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * INSERT-first, not exists-then-insert: 2 instances receiving the same
 * duplicate message at nearly the same time would both see exists=false under
 * a check-then-act approach (race window). Inserting immediately and catching
 * the PK violation lets the DB constraint act as the single arbiter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private static final Duration RETENTION = Duration.ofDays(7);

    private final ProcessedEventRepository processedEventRepository;

    /** @return true if this is the first time (event_id, consumer) is seen; false if it's a duplicate. */
    @Transactional
    public boolean markProcessedIfNew(String eventId, String consumer) {
        try {
            // saveAndFlush (not save) — the constraint violation must surface inside this
            // try block. A plain save() only flushes at transaction commit, which happens
            // after this method returns, so the exception would escape uncaught.
            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, consumer, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event, skipping: eventId={}, consumer={}", eventId, consumer);
            return false;
        }
    }

    /** Retention must be >= the Kafka topic's retention — a message can't be re-read after it has left the topic. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldEvents() {
        int deleted = processedEventRepository.deleteByProcessedAtBefore(Instant.now().minus(RETENTION));
        if (deleted > 0) {
            log.info("Cleaned up {} processed_event row(s) older than {} days", deleted, RETENTION.toDays());
        }
    }
}
