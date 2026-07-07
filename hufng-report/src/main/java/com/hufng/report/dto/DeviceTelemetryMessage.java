package com.hufng.report.dto;

import java.time.Instant;

/**
 * Mirrors com.hufng.ingest.dto.DeviceTelemetryMessage field-for-field — the two
 * modules don't share a jar, so the JSON shape on the DEVICE_TELEMETRY topic is
 * the actual contract between them.
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
