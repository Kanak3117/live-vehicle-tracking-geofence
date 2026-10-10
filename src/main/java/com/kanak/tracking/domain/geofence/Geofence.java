package com.kanak.tracking.domain.geofence;

import java.time.LocalTime;
import java.util.Set;

public abstract class Geofence {
    private final Long id;
    private final LocalTime activeFrom;          // null = always active
    private final LocalTime activeTo;
    private final Set<Long> allowedVehicleIds;   // empty = all vehicles

    protected Geofence(Long id, LocalTime activeFrom, LocalTime activeTo, Set<Long> allowedVehicleIds) {
        this.id = id;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        this.allowedVehicleIds = allowedVehicleIds == null ? Set.of() : Set.copyOf(allowedVehicleIds);
    }

    public abstract boolean contains(double lat, double lon);
    public abstract BoundingBox boundingBox();

    /** Handles wrap-around windows like 21:00-05:00. */
    public final boolean isActiveAt(LocalTime t) {
        if (activeFrom == null || activeTo == null || activeFrom.equals(activeTo)) return true;
        if (activeFrom.isBefore(activeTo)) {
            return !t.isBefore(activeFrom) && t.isBefore(activeTo);
        }
        return !t.isBefore(activeFrom) || t.isBefore(activeTo);   // wraps past midnight
    }

    public final boolean appliesTo(long vehicleId) {
        return allowedVehicleIds.isEmpty() || allowedVehicleIds.contains(vehicleId);
    }

    public Long getId() { return id; }
}