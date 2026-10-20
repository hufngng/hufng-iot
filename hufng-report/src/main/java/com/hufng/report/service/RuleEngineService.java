package com.hufng.report.service;

import com.hufng.report.config.AlertRuleProperties;
import com.hufng.report.dto.AlertRule;
import com.hufng.report.dto.DeviceTelemetryMessage;
import com.hufng.report.dto.ResourceAlertMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    private final AlertRuleProperties alertRuleProperties;
    private final KafkaTemplate<String, ResourceAlertMessage> kafkaTemplate;

    private Map<String, AlertRule> rulesByMetric;

    public RuleEngineService(AlertRuleProperties alertRuleProperties,
                              KafkaTemplate<String, ResourceAlertMessage> kafkaTemplate) {
        this.alertRuleProperties = alertRuleProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    void loadRules() {
        rulesByMetric = new HashMap<>();
        for (AlertRule rule : alertRuleProperties.getAlertRules()) {
            rulesByMetric.put(rule.metric(), rule);
        }
        log.info("Loaded {} alert rule(s): {}", rulesByMetric.size(), rulesByMetric.keySet());
    }

    public void check(DeviceTelemetryMessage message) {
        AlertRule rule = rulesByMetric.get(message.metric());
        if (rule == null || message.value() <= rule.threshold()) {
            return;
        }

        String content = "Device %s: %s = %.2f exceeded threshold %.2f"
                .formatted(message.deviceId(), message.metric(), message.value(), rule.threshold());
        ResourceAlertMessage alert = new ResourceAlertMessage(UUID.randomUUID().toString(), rule.channel(), content);
        kafkaTemplate.send("RESOURCE_ALERT", message.deviceId(), alert);
    }
}
