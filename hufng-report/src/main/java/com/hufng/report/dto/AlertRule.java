package com.hufng.report.dto;

public record AlertRule(String metric, double threshold, AlertChannel channel) {
}
