package com.hufng.ingest.dto;

import com.hufng.ingest.enums.DeviceStatus;

import java.time.Instant;

/** deviceSecret is returned in plaintext exactly once — only the bcrypt hash is persisted. */
public record DeviceRegisterResponse(String deviceId, String deviceSecret, DeviceStatus status, Instant createdAt) {
}
