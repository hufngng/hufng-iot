package com.hufng.report.consumer;

import com.hufng.report.dto.DeviceTelemetryMessage;
import com.hufng.report.repository.TelemetryRepository;
import com.hufng.report.service.RuleEngineService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DeviceTelemetryConsumer {

    private final TelemetryRepository telemetryRepository;
    private final RuleEngineService ruleEngineService;

    public DeviceTelemetryConsumer(TelemetryRepository telemetryRepository, RuleEngineService ruleEngineService) {
        this.telemetryRepository = telemetryRepository;
        this.ruleEngineService = ruleEngineService;
    }

    @KafkaListener(topics = "DEVICE_TELEMETRY", groupId = "hufng-report")
    public void consume(DeviceTelemetryMessage message, Acknowledgment ack) {
        try {
            telemetryRepository.insert(message);
            ruleEngineService.check(message);
        } finally {
            ack.acknowledge();
        }
    }
}
