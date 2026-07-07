package com.hufng.ingest.dto;

/** Raw JSON body published by the device over MQTT. */
public record DeviceTelemetryPayload(
        String deviceId,
        Long seq,
        String eventTime,
        String metric,
        Double value,
        String auth
) {

    public boolean hasRequiredFields() {
        return deviceId != null && !deviceId.isBlank()
                && seq != null
                && eventTime != null && !eventTime.isBlank()
                && metric != null && !metric.isBlank()
                && value != null;
    }
}
