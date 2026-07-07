package com.hufng.ingest.dto;

import com.hufng.ingest.enums.DeviceStatus;

import java.io.Serializable;

/** Cached in Redis under key "device:{deviceId}", TTL 1h — only ever cached when status == ACTIVE. */
public record DeviceCacheEntry(String deviceId, DeviceStatus status, String secretHash) implements Serializable {
}
