package com.hufng.report.consumer;

import com.hufng.report.dto.DeviceTelemetryMessage;
import com.hufng.report.repository.TelemetryRepository;
import com.hufng.report.service.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceTelemetryConsumerTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private Acknowledgment ack;

    private DeviceTelemetryConsumer consumer;

    private final DeviceTelemetryMessage message = new DeviceTelemetryMessage(
            "sensor-1", 7, Instant.parse("2026-10-21T00:00:00Z"), "temperature", 25.0, Instant.parse("2026-10-21T00:00:01Z"));

    @BeforeEach
    void setUp() {
        consumer = new DeviceTelemetryConsumer(telemetryRepository, ruleEngineService);
    }

    @Test
    void consume_storesTelemetry_thenChecksRules_thenAcknowledges() {
        consumer.consume(message, ack);

        InOrder order = inOrder(telemetryRepository, ruleEngineService, ack);
        order.verify(telemetryRepository).insert(message);
        order.verify(ruleEngineService).check(message);
        order.verify(ack).acknowledge();
    }

    @Test
    void consume_insertFails_stillAcknowledgesAndSkipsRuleCheck() {
        doThrow(new RuntimeException("clickhouse down")).when(telemetryRepository).insert(message);

        assertThatThrownBy(() -> consumer.consume(message, ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("clickhouse down");

        verify(ruleEngineService, never()).check(any());
        verify(ack).acknowledge();
    }

    @Test
    void consume_ruleCheckFails_stillAcknowledges() {
        doThrow(new RuntimeException("kafka send failed")).when(ruleEngineService).check(message);

        assertThatThrownBy(() -> consumer.consume(message, ack))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("kafka send failed");

        verify(telemetryRepository).insert(message);
        verify(ack).acknowledge();
    }
}
