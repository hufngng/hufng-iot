package com.hufng.ingest.service;

import com.hufng.common.service.RedisService;
import com.hufng.ingest.dto.DeviceCacheEntry;
import com.hufng.ingest.entity.Device;
import com.hufng.ingest.enums.DeviceStatus;
import com.hufng.ingest.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAuthServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DeviceAuthService deviceAuthService;

    @Test
    void authenticate_cacheHit_secretMatches_returnsOk() {
        when(redisService.get("device:sensor-1"))
                .thenReturn(new DeviceCacheEntry("sensor-1", DeviceStatus.ACTIVE, "hashed"));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(deviceRepository.findById("sensor-1"))
                .thenReturn(Optional.of(new Device("sensor-1", 1L, "s1", "hashed", DeviceStatus.ACTIVE, "DHT22", Instant.now(), null)));

        DeviceAuthResult result = deviceAuthService.authenticate("sensor-1", "secret");

        assertThat(result).isEqualTo(DeviceAuthResult.OK);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void authenticate_cacheMiss_deviceNotFound_returnsNotFoundOrRevoked() {
        when(redisService.get("device:unknown")).thenReturn(null);
        when(deviceRepository.findById("unknown")).thenReturn(Optional.empty());

        DeviceAuthResult result = deviceAuthService.authenticate("unknown", "secret");

        assertThat(result).isEqualTo(DeviceAuthResult.NOT_FOUND_OR_REVOKED);
    }

    @Test
    void authenticate_corruptCacheEntry_fallsBackToDbInsteadOfThrowing() {
        when(redisService.get("device:sensor-1"))
                .thenThrow(new org.springframework.data.redis.serializer.SerializationException("bad payload"));
        when(deviceRepository.findById("sensor-1"))
                .thenReturn(Optional.of(new Device("sensor-1", 1L, "s1", "hashed", DeviceStatus.ACTIVE, "DHT22", Instant.now(), null)));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        DeviceAuthResult result = deviceAuthService.authenticate("sensor-1", "secret");

        assertThat(result).isEqualTo(DeviceAuthResult.OK);
        verify(redisService).del("device:sensor-1");
    }

    @Test
    void authenticate_secretMismatch_returnsAuthMismatch() {
        when(redisService.get("device:sensor-1"))
                .thenReturn(new DeviceCacheEntry("sensor-1", DeviceStatus.ACTIVE, "hashed"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        DeviceAuthResult result = deviceAuthService.authenticate("sensor-1", "wrong-secret");

        assertThat(result).isEqualTo(DeviceAuthResult.AUTH_MISMATCH);
    }
}
