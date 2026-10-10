package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.GeofenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, Long> {
    List<GeofenceEvent> findByVehicleIdAndEventTimeBetween(Long vehicleId, Instant from, Instant to);
}