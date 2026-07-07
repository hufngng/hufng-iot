package com.hufng.ingest.repository;

import com.hufng.ingest.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
}
