package com.hufng.report.repository;

import com.hufng.report.dto.DeviceTelemetryMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * ClickHouse has no UNIQUE constraint — a duplicate row lands in the table
 * regardless, and is only merged away later by the background merge on
 * ReplacingMergeTree(_version). Dedupe here is eventual, not write-time; that's
 * an accepted trade-off for telemetry (losing/duplicating one data point
 * doesn't matter), unlike the exact write-time dedupe hufng-notification uses
 * for the alert side-effect (MySQL processed_event UNIQUE).
 */
@Repository
public class TelemetryRepository {

    private static final String INSERT_SQL = """
            INSERT INTO telemetry (device_id, seq, metric, value, event_time, ingestion_time, _version)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public TelemetryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(DeviceTelemetryMessage message) {
        Instant ingestionTime = Instant.now();
        jdbcTemplate.update(INSERT_SQL,
                message.deviceId(),
                message.seq(),
                message.metric(),
                message.value(),
                Timestamp.from(message.eventTime()),
                Timestamp.from(ingestionTime),
                // _version: latest ingestion wins when the same (device_id, seq) is merged —
                // a retried duplicate re-ingested later should replace the earlier row.
                ingestionTime.toEpochMilli());
    }
}
