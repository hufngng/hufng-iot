package com.hufng.report.config;

import com.hufng.report.dto.AlertChannel;
import com.hufng.report.dto.AlertRule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRulePropertiesTest {

    @Test
    void defaultsToNoRules() {
        assertThat(new AlertRuleProperties().getAlertRules()).isEmpty();
    }

    @Test
    void bindsAlertRulesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "report.alert-rules[0].metric", "temperature",
                "report.alert-rules[0].threshold", "30",
                "report.alert-rules[0].channel", "TELEGRAM",
                "report.alert-rules[1].metric", "cpu",
                "report.alert-rules[1].threshold", "90.5",
                "report.alert-rules[1].channel", "discord"));

        AlertRuleProperties properties = new Binder(source)
                .bind("report", Bindable.ofInstance(new AlertRuleProperties()))
                .get();

        assertThat(properties.getAlertRules()).containsExactly(
                new AlertRule("temperature", 30, AlertChannel.TELEGRAM),
                // Spring's relaxed binding accepts lower-case enum values
                new AlertRule("cpu", 90.5, AlertChannel.DISCORD));
    }
}
