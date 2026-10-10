package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.Device;
import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.eta.SpeedEstimator;
import com.kanak.tracking.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class PingIngestionService {

    private final DeviceRepository deviceRepository;
    private final LiveLocationStore store;
    private final GeofenceEvaluationService geofenceService;
    private final SpeedEstimator speedEstimator;
    private final Clock clock;
    private final Duration staleLimit;

    public PingIngestionService(DeviceRepository deviceRepository,
                                LiveLocationStore store,
                                GeofenceEvaluationService geofenceService,
                                SpeedEstimator speedEstimator,
                                Clock clock,
                                @Value("${tracking.ping.stale-seconds:300}") long staleSeconds) {
        this.deviceRepository = deviceRepository;
        this.store = store;
        this.geofenceService = geofenceService;
        this.speedEstimator = speedEstimator;
        this.clock = clock;
        this.staleLimit = Duration.ofSeconds(staleSeconds);
    }

    public PingResult ingest(String deviceId, double lat, double lon,
                             double bearing, double speed, Instant timestamp) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }

        Long vehicleId = deviceRepository.findById(deviceId)
                .map(Device::getVehicleId)
                .orElse(null);
        if (vehicleId == null) {
            return PingResult.UNKNOWN_DEVICE;
        }

        if (timestamp.isBefore(clock.instant().minus(staleLimit))) {
            return PingResult.STALE;
        }

        LocationFix fix = new LocationFix(vehicleId, lat, lon, bearing, speed, timestamp);
        switch (store.offer(fix)) {
            case UPDATED -> {
                speedEstimator.update(vehicleId, speed);
                geofenceService.evaluate(fix);
                return PingResult.ACCEPTED;
            }
            case DUPLICATE -> {
                return PingResult.DUPLICATE;
            }
            default -> {
                return PingResult.OUT_OF_ORDER;
            }
        }
    }
}