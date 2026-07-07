package com.hufng.ingest.service;

import com.hufng.common.service.RedisService;
import com.hufng.ingest.dto.DeviceCacheEntry;
import com.hufng.ingest.entity.Device;
import com.hufng.ingest.enums.DeviceStatus;
import com.hufng.ingest.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Device auth check + registry cache. Every message hitting the DB would cost
 * N x throughput, so an ACTIVE device's (status, secretHash) projection is
 * cached in Redis for 1h; revoke = update DB + evict cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAuthService {

    private static final long CACHE_TTL_SECONDS = 3600;
    private static final String CACHE_KEY_PREFIX = "device:";

    private final DeviceRepository deviceRepository;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    public DeviceAuthResult authenticate(String deviceId, String authToken) {
        DeviceCacheEntry entry = loadCacheEntry(deviceId);
        if (entry == null) {
            log.warn("Device not registered or revoked, skipping message: deviceId={}", deviceId);
            return DeviceAuthResult.NOT_FOUND_OR_REVOKED;
        }

        if (!passwordEncoder.matches(authToken == null ? "" : authToken, entry.secretHash())) {
            log.warn("Device auth failed, skipping message: deviceId={}", deviceId);
            return DeviceAuthResult.AUTH_MISMATCH;
        }

        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setLastSeenAt(Instant.now());
            deviceRepository.save(device);
        });
        return DeviceAuthResult.OK;
    }

    public void cacheActiveDevice(String deviceId, String secretHash) {
        redisService.set(cacheKey(deviceId), new DeviceCacheEntry(deviceId, DeviceStatus.ACTIVE, secretHash), CACHE_TTL_SECONDS);
    }

    public void evict(String deviceId) {
        redisService.del(cacheKey(deviceId));
    }

    private DeviceCacheEntry loadCacheEntry(String deviceId) {
        Object cached = redisService.get(cacheKey(deviceId));
        if (cached instanceof DeviceCacheEntry entry) {
            return entry;
        }

        return deviceRepository.findById(deviceId)
                .filter(device -> device.getStatus() == DeviceStatus.ACTIVE)
                .map(device -> {
                    DeviceCacheEntry entry = new DeviceCacheEntry(deviceId, DeviceStatus.ACTIVE, device.getDeviceSecretHash());
                    redisService.set(cacheKey(deviceId), entry, CACHE_TTL_SECONDS);
                    return entry;
                })
                .orElse(null);
    }

    private String cacheKey(String deviceId) {
        return CACHE_KEY_PREFIX + deviceId;
    }
}
