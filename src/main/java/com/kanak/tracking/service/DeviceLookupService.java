package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.Device;
import com.kanak.tracking.repository.DeviceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DeviceLookupService {

    private final DeviceRepository repository;

    public DeviceLookupService(DeviceRepository repository) {
        this.repository = repository;
    }

    /** Hot path: called on every ping. Hits are cached; unknown devices are not. */
    @Cacheable(value = "devices", unless = "#result == null")
    public Long findVehicleId(String deviceId) {
        return repository.findById(deviceId).map(Device::getVehicleId).orElse(null);
    }
}