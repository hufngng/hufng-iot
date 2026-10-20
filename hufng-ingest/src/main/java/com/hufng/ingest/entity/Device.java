package com.hufng.ingest.entity;

import com.hufng.ingest.enums.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "device_name", length = 128)
    private String deviceName;

    @Column(name = "device_secret_hash", nullable = false)
    private String deviceSecretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus status;

    @Column(length = 64)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public Device() {
    }

    public Device(String deviceId, Long ownerId, String deviceName, String deviceSecretHash,
                  DeviceStatus status, String model, Instant createdAt, Instant lastSeenAt) {
        this.deviceId = deviceId;
        this.ownerId = ownerId;
        this.deviceName = deviceName;
        this.deviceSecretHash = deviceSecretHash;
        this.status = status;
        this.model = model;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceSecretHash() {
        return deviceSecretHash;
    }

    public void setDeviceSecretHash(String deviceSecretHash) {
        this.deviceSecretHash = deviceSecretHash;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
