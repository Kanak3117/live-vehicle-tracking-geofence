package com.kanak.tracking.service;

import com.kanak.tracking.domain.geofence.CircleGeofence;
import com.kanak.tracking.domain.geofence.Geofence;
import com.kanak.tracking.domain.geofence.RectGeofence;
import com.kanak.tracking.domain.model.GeofenceEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GeofenceMapper {

    public Geofence toDomain(GeofenceEntity e) {
        if (e.getType() == null) {
            throw new IllegalArgumentException("Geofence type is required (CIRCLE or RECT)");
        }
        Set<Long> vehicles = e.getAllowedVehicleIds() == null ? Set.of() : e.getAllowedVehicleIds();

        switch (e.getType()) {
            case CIRCLE -> {
                require(e.getType().name(), e.getCenterLat(), e.getCenterLon(), e.getRadiusM());
                return new CircleGeofence(e.getId(), e.getCenterLat(), e.getCenterLon(), e.getRadiusM(),
                        e.getActiveFrom(), e.getActiveTo(), vehicles);
            }
            case RECT -> {
                require(e.getType().name(), e.getMinLat(), e.getMinLon(), e.getMaxLat(), e.getMaxLon());
                return new RectGeofence(e.getId(), e.getMinLat(), e.getMinLon(), e.getMaxLat(), e.getMaxLon(),
                        e.getActiveFrom(), e.getActiveTo(), vehicles);
            }
            default -> throw new IllegalArgumentException("Unsupported geofence type");
        }
    }

    private static void require(String type, Double... values) {
        for (Double v : values) {
            if (v == null) {
                throw new IllegalArgumentException("Missing required fields for " + type + " geofence");
            }
        }
    }
}