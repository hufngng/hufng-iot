package com.hufng.report.repository;

import com.hufng.report.dto.DeviceTelemetryMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelemetryRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TelemetryRepository telemetryRepository;

    @Test
    void insert_bindsMessageFieldsInColumnOrder() {
        Instant eventTime = Instant.parse("2026-10-21T08:00:00Z");
        // ingestionTime on the message (set by hufng-ingest) is ignored — the repository stamps its own
        Instant upstreamIngestionTime = Instant.parse("2000-01-01T00:00:00Z");
        DeviceTelemetryMessage message = new DeviceTelemetryMessage(
                "sensor-1", 42, eventTime, "temperature", 36.6, upstreamIngestionTime);

        long before = System.currentTimeMillis();
        telemetryRepository.insert(message);
        long after = System.currentTimeMillis();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(sql.capture(), args.capture(), args.capture(), args.capture(),
                args.capture(), args.capture(), args.capture(), args.capture());

        assertThat(sql.getValue())
                .contains("INSERT INTO telemetry")
                .contains("(device_id, seq, metric, value, event_time, ingestion_time, _version)");

        Object[] params = args.getAllValues().toArray();
        assertThat(params[0]).isEqualTo("sensor-1");
        assertThat(params[1]).isEqualTo(42L);
        assertThat(params[2]).isEqualTo("temperature");
        assertThat(params[3]).isEqualTo(36.6);
        assertThat(params[4]).isEqualTo(Timestamp.from(eventTime));

        Timestamp ingestionTime = (Timestamp) params[5];
        assertThat(ingestionTime.getTime()).isBetween(before, after);

        // _version = ingestion time in epoch millis, so a later re-ingest wins the ReplacingMergeTree merge
        assertThat(params[6]).isEqualTo(ingestionTime.getTime());
    }
}
