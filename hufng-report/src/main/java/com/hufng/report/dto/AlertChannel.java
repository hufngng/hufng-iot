package com.hufng.report.dto;

/** Mirrors com.hufng.notification.enums.NotificationChannel — same names, serialized as the JSON string Jackson expects on the other side. */
public enum AlertChannel {
    TELEGRAM,
    DISCORD,
    ALL
}
