package com.hufng.report.service;

import com.hufng.report.config.AlertRuleProperties;
import com.hufng.report.dto.AlertChannel;
import com.hufng.report.dto.AlertRule;
import com.hufng.report.dto.DeviceTelemetryMessage;
import com.hufng.report.dto.ResourceAlertMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private KafkaTemplate<String, ResourceAlertMessage> kafkaTemplate;

    private RuleEngineService ruleEngineService;

    // Alert content uses %.2f with the JVM default locale — pin it so "42.00" doesn't become "42,00"
    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        ruleEngineService = serviceWith(List.of(new AlertRule("temperature", 30, AlertChannel.TELEGRAM)));
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void check_valueBelowThreshold_doesNotPublishAlert() {
        ruleEngineService.check(telemetry("sensor-1", "temperature", 20.0));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void check_valueEqualToThreshold_doesNotPublishAlert() {
        // Rule fires only when value is strictly greater than threshold
        ruleEngineService.check(telemetry("sensor-1", "temperature", 30.0));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void check_valueAboveThreshold_publishesAlert() {
        ruleEngineService.check(telemetry("sensor-1", "temperature", 42.0));

        ArgumentCaptor<ResourceAlertMessage> captor = ArgumentCaptor.forClass(ResourceAlertMessage.class);
        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("sensor-1"), captor.capture());
        ResourceAlertMessage alert = captor.getValue();
        assertThat(alert.channel()).isEqualTo(AlertChannel.TELEGRAM);
        assertThat(alert.eventId()).isNotBlank();
        assertThat(alert.content()).isEqualTo("Device sensor-1: temperature = 42.00 exceeded threshold 30.00");
    }

    @Test
    void check_valueJustAboveThreshold_publishesAlert() {
        ruleEngineService.check(telemetry("sensor-1", "temperature", 30.001));

        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("sensor-1"), any(ResourceAlertMessage.class));
    }

    @Test
    void check_unknownMetric_doesNotPublishAlert() {
        ruleEngineService.check(telemetry("sensor-1", "humidity", 99.0));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void check_noRulesConfigured_doesNotPublishAlert() {
        RuleEngineService service = serviceWith(List.of());

        service.check(telemetry("sensor-1", "temperature", 1000.0));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void check_multipleRules_usesRuleMatchingTheMetric() {
        RuleEngineService service = serviceWith(List.of(
                new AlertRule("temperature", 30, AlertChannel.TELEGRAM),
                new AlertRule("cpu", 90, AlertChannel.DISCORD)));

        service.check(telemetry("server-7", "cpu", 95.5));

        ArgumentCaptor<ResourceAlertMessage> captor = ArgumentCaptor.forClass(ResourceAlertMessage.class);
        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("server-7"), captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(AlertChannel.DISCORD);
        assertThat(captor.getValue().content()).contains("cpu = 95.50", "threshold 90.00");
    }

    @Test
    void check_duplicateMetricRules_lastRuleWins() {
        RuleEngineService service = serviceWith(List.of(
                new AlertRule("temperature", 30, AlertChannel.TELEGRAM),
                new AlertRule("temperature", 50, AlertChannel.ALL)));

        // 40 exceeds the first rule but not the second, which overrides it
        service.check(telemetry("sensor-1", "temperature", 40.0));
        verify(kafkaTemplate, never()).send(any(), any(), any());

        service.check(telemetry("sensor-1", "temperature", 60.0));
        ArgumentCaptor<ResourceAlertMessage> captor = ArgumentCaptor.forClass(ResourceAlertMessage.class);
        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("sensor-1"), captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(AlertChannel.ALL);
    }

    @Test
    void check_eachAlertGetsAFreshEventId() {
        // eventId is the notification-side dedupe key: two distinct breaches must not share one
        ruleEngineService.check(telemetry("sensor-1", "temperature", 42.0));
        ruleEngineService.check(telemetry("sensor-1", "temperature", 43.0));

        ArgumentCaptor<ResourceAlertMessage> captor = ArgumentCaptor.forClass(ResourceAlertMessage.class);
        verify(kafkaTemplate, times(2)).send(eq("RESOURCE_ALERT"), eq("sensor-1"), captor.capture());
        List<ResourceAlertMessage> alerts = captor.getAllValues();
        assertThat(alerts.get(0).eventId()).isNotEqualTo(alerts.get(1).eventId());
    }

    @Test
    void check_usesDeviceIdAsKafkaKey() {
        // Keying by deviceId keeps all alerts of one device on the same partition (ordered)
        ruleEngineService.check(telemetry("sensor-A", "temperature", 42.0));
        ruleEngineService.check(telemetry("sensor-B", "temperature", 42.0));

        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("sensor-A"), any(ResourceAlertMessage.class));
        verify(kafkaTemplate).send(eq("RESOURCE_ALERT"), eq("sensor-B"), any(ResourceAlertMessage.class));
    }

    private RuleEngineService serviceWith(List<AlertRule> rules) {
        AlertRuleProperties properties = new AlertRuleProperties();
        properties.setAlertRules(rules);
        RuleEngineService service = new RuleEngineService(properties, kafkaTemplate);
        service.loadRules();
        return service;
    }

    private static DeviceTelemetryMessage telemetry(String deviceId, String metric, double value) {
        return new DeviceTelemetryMessage(deviceId, 1, Instant.now(), metric, value, Instant.now());
    }
}
