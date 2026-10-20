package com.hufng.ingest.controller;

import com.hufng.ingest.dto.DeviceRegisterRequest;
import com.hufng.ingest.dto.DeviceRegisterResponse;
import com.hufng.ingest.entity.Device;
import com.hufng.ingest.enums.DeviceStatus;
import com.hufng.ingest.repository.DeviceRepository;
import com.hufng.ingest.service.DeviceAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/devices")
public class DeviceRegistrationController {

    private final DeviceRepository deviceRepository;
    private final DeviceAuthService deviceAuthService;
    private final PasswordEncoder passwordEncoder;

    public DeviceRegistrationController(DeviceRepository deviceRepository,
                                         DeviceAuthService deviceAuthService,
                                         PasswordEncoder passwordEncoder) {
        this.deviceRepository = deviceRepository;
        this.deviceAuthService = deviceAuthService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceRegisterResponse register(@RequestBody DeviceRegisterRequest request) {
        String deviceId = "sensor-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String secret = UUID.randomUUID().toString();
        String secretHash = passwordEncoder.encode(secret);
        Instant now = Instant.now();

        Device device = new Device(deviceId, request.ownerId(), request.deviceName(),
                secretHash, DeviceStatus.ACTIVE, request.model(), now, null);
        deviceRepository.save(device);

        // Warm the cache immediately so the first telemetry message doesn't pay a DB miss.
        deviceAuthService.cacheActiveDevice(deviceId, secretHash);

        return new DeviceRegisterResponse(deviceId, secret, DeviceStatus.ACTIVE, now);
    }
}
