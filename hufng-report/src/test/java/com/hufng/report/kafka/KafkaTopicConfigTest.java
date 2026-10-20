package com.hufng.report.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicConfigTest {

    private final KafkaTopicConfig config = new KafkaTopicConfig();

    @Test
    void deviceTelemetryTopic_matchesIngestDeclaration() {
        // Must match hufng-ingest's declaration, otherwise KafkaAdmin sees conflicting definitions
        NewTopic topic = config.deviceTelemetryTopic();

        assertThat(topic.name()).isEqualTo("DEVICE_TELEMETRY");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void resourceAlertTopic() {
        NewTopic topic = config.resourceAlertTopic();

        assertThat(topic.name()).isEqualTo("RESOURCE_ALERT");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }
}
