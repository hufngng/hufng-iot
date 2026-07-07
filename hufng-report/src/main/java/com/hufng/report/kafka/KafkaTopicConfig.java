package com.hufng.report.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * DEVICE_TELEMETRY is also declared by hufng-ingest (the producer); declaring it
 * again here with the same name/partition count is a harmless no-op for
 * KafkaAdmin. RESOURCE_ALERT is declared here since hufng-report is the
 * producer for that topic.
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

    @Bean
    public NewTopic resourceAlertTopic() {
        return TopicBuilder.name("RESOURCE_ALERT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
