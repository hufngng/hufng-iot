package com.hufng.ingest.dto;

/**
 * ownerId is taken directly from the request body instead of a JWT principal —
 * hufng-ingest intentionally does not depend on hufng-auth in this iteration.
 * A fuller system would resolve owner from an authenticated user session.
 */
public record DeviceRegisterRequest(String deviceName, String model, Long ownerId) {
}
