package com.hufng.report.service;

import com.hufng.report.config.AlertRuleProperties;
import com.hufng.report.dto.AlertChannel;
import com.hufng.report.dto.AlertRule;
import com.hufng.report.dto.DeviceTelemetryMessage;
import com.hufng.report.dto.ResourceAlertMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, ResourceAlertMessage> kafkaTemplate;

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        AlertRuleProperties properties = new AlertRuleProperties();
        properties.setAlertRules(List.of(new AlertRule("temperature", 30, AlertChannel.TELEGRAM)));
        ruleEngineService = new RuleEngineService(properties, kafkaTemplate);
        ruleEngineService.loadRules();
    }

    @Test
    void check_valueBelowThreshold_doesNotPublishAlert() {
        DeviceTelemetryMessage message = new DeviceTelemetryMessage(
                "sensor-1", 1, Instant.now(), "temperature", 20.0, Instant.now());

        ruleEngineService.check(message);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void check_valueAboveThreshold_publishesAlert() {
        DeviceTelemetryMessage message = new DeviceTelemetryMessage(
                "sensor-1", 1, Instant.now(), "temperature", 42.0, Instant.now());

        ruleEngineService.check(message);

        ArgumentCaptor<ResourceAlertMessage> captor = ArgumentCaptor.forClass(ResourceAlertMessage.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("RESOURCE_ALERT"),
                org.mockito.ArgumentMatchers.eq("sensor-1"), captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(AlertChannel.TELEGRAM);
        assertThat(captor.getValue().eventId()).isNotBlank();
    }

    @Test
    void check_unknownMetric_doesNotPublishAlert() {
        DeviceTelemetryMessage message = new DeviceTelemetryMessage(
                "sensor-1", 1, Instant.now(), "humidity", 99.0, Instant.now());

        ruleEngineService.check(message);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
