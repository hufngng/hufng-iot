package com.hufng.report.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.JacksonUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The DTOs are not shared across modules — the JSON shape on the topics is the contract.
 * Uses the same ObjectMapper Spring Kafka's JsonSerializer/JsonDeserializer build by default.
 */
class KafkaMessageContractTest {

    private final ObjectMapper objectMapper = JacksonUtils.enhancedObjectMapper();

    @Test
    void deviceTelemetryMessage_deserializesFromIngestJson() throws Exception {
        String json = """
                {
                  "deviceId": "sensor-1",
                  "seq": 42,
                  "eventTime": "2026-10-21T08:00:00Z",
                  "metric": "temperature",
                  "value": 36.6,
                  "ingestionTime": "2026-10-21T08:00:01.500Z",
                  "unknownField": "ignored"
                }
                """;

        DeviceTelemetryMessage message = objectMapper.readValue(json, DeviceTelemetryMessage.class);

        assertThat(message).isEqualTo(new DeviceTelemetryMessage(
                "sensor-1", 42, Instant.parse("2026-10-21T08:00:00Z"), "temperature", 36.6,
                Instant.parse("2026-10-21T08:00:01.500Z")));
    }

    @Test
    void resourceAlertMessage_serializesChannelAsEnumName() throws Exception {
        ResourceAlertMessage alert = new ResourceAlertMessage("evt-1", AlertChannel.DISCORD, "hot");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(alert));

        assertThat(json.get("eventId").asText()).isEqualTo("evt-1");
        assertThat(json.get("channel").asText()).isEqualTo("DISCORD");
        assertThat(json.get("content").asText()).isEqualTo("hot");
    }
}
