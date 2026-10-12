package com.kanak.tracking.dto;

import com.kanak.tracking.domain.model.GeofenceEntity;
import com.kanak.tracking.domain.model.GeofenceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

public record GeofenceRequest(
        String name,
        @NotNull GeofenceType type,
        Double centerLat, Double centerLon, Double radiusM,
        Double minLat, Double minLon, Double maxLat, Double maxLon,
        LocalTime activeFrom, LocalTime activeTo,
        Set<Long> allowedVehicleIds) {

    public GeofenceEntity toEntity() {
        GeofenceEntity e = new GeofenceEntity();
        e.setName(name);
        e.setType(type);
        e.setCenterLat(centerLat);
        e.setCenterLon(centerLon);
        e.setRadiusM(radiusM);
        e.setMinLat(minLat);
        e.setMinLon(minLon);
        e.setMaxLat(maxLat);
        e.setMaxLon(maxLon);
        e.setActiveFrom(activeFrom);
        e.setActiveTo(activeTo);
        e.setAllowedVehicleIds(allowedVehicleIds == null ? new HashSet<>() : new HashSet<>(allowedVehicleIds));
        return e;
    }
}