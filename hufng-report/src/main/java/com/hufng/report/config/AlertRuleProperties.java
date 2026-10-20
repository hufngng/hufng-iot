package com.hufng.report.config;

import com.hufng.report.dto.AlertRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Rule engine kept intentionally minimal: rules are loaded once from
 * application.yml at startup, not from a DB table checked per message.
 * A production-scale version would move to Kafka Streams/Flink rather than
 * per-message rule evaluation in a single consumer.
 */
@Configuration
@ConfigurationProperties(prefix = "report")
public class AlertRuleProperties {

    private List<AlertRule> alertRules = List.of();

    public List<AlertRule> getAlertRules() {
        return alertRules;
    }

    public void setAlertRules(List<AlertRule> alertRules) {
        this.alertRules = alertRules;
    }
}
