package com.hufng.ingest.dto;

import java.time.Instant;

/**
 * Payload of the DEVICE_TELEMETRY Kafka topic (key = deviceId).
 * eventTime is when the device measured the value; ingestionTime is when this
 * bridge received it — hufng-report aggregates by eventTime, not ingestionTime,
 * so late/out-of-order device batches still land in the right time bucket.
 */
public record DeviceTelemetryMessage(
        String deviceId,
        long seq,
        Instant eventTime,
        String metric,
        double value,
        Instant ingestionTime
) {
}
