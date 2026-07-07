package com.hufng.ingest.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares DEVICE_TELEMETRY explicitly instead of relying on broker auto-create,
 * so partition count is controlled from day one (key = deviceId -> per-device order).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic deviceTelemetryTopic() {
        return TopicBuilder.name("DEVICE_TELEMETRY")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
