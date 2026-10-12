package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.GeofenceEvent;
import com.kanak.tracking.repository.GeofenceEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class GeofenceEventService {

    private final GeofenceEventRepository repository;

    public GeofenceEventService(GeofenceEventRepository repository) {
        this.repository = repository;
    }

    public List<GeofenceEvent> find(Long vehicleId, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        return repository.findByVehicleIdAndEventTimeBetween(vehicleId, from, to);
    }
}