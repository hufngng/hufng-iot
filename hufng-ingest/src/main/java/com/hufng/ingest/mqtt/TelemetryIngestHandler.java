package com.hufng.ingest.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufng.ingest.dto.DeviceTelemetryMessage;
import com.hufng.ingest.dto.DeviceTelemetryPayload;
import com.hufng.ingest.service.DeviceAuthResult;
import com.hufng.ingest.service.DeviceAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Does 5 things per message: parse -> validate device -> auth token -> validate
 * schema -> produce Kafka. Any failure logs a warning and drops the message —
 * MQTT QoS 1 has already delivered it, we just choose not to forward it, we never
 * throw back into the Paho adapter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryIngestHandler {

    private final ObjectMapper objectMapper;
    private final DeviceAuthService deviceAuthService;
    private final KafkaTemplate<String, DeviceTelemetryMessage> kafkaTemplate;

    public void handle(Message<?> message) {
        String body = String.valueOf(message.getPayload());
        DeviceTelemetryPayload payload;
        try {
            payload = objectMapper.readValue(body, DeviceTelemetryPayload.class);
        } catch (Exception e) {
            log.warn("Failed to parse MQTT telemetry payload, dropping: {}", body, e);
            return;
        }

        if (!payload.hasRequiredFields()) {
            log.warn("Telemetry payload missing required fields, dropping: {}", payload);
            return;
        }

        DeviceAuthResult authResult = deviceAuthService.authenticate(payload.deviceId(), payload.auth());
        if (authResult != DeviceAuthResult.OK) {
            return;
        }

        DeviceTelemetryMessage telemetryMessage = new DeviceTelemetryMessage(
                payload.deviceId(),
                payload.seq(),
                Instant.parse(payload.eventTime()),
                payload.metric(),
                payload.value(),
                Instant.now());

        kafkaTemplate.send("DEVICE_TELEMETRY", telemetryMessage.deviceId(), telemetryMessage);
    }
}
