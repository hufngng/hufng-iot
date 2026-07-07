package com.hufng.report.dto;

/**
 * Mirrors com.hufng.notification.dto.ResourceAlertMessage field-for-field.
 * eventId is generated HERE (the producer) — never by the consumer, otherwise
 * the same logical alert re-read after a rebalance would get a different id
 * each time and the notification-side dedupe would be meaningless.
 */
public record ResourceAlertMessage(String eventId, AlertChannel channel, String content) {
}
