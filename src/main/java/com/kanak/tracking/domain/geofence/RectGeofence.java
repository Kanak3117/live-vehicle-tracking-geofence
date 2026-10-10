package com.kanak.tracking.domain.geofence;

import java.time.LocalTime;
import java.util.Set;

public class RectGeofence extends Geofence {
    private final BoundingBox box;

    public RectGeofence(Long id, double minLat, double minLon, double maxLat, double maxLon,
                        LocalTime from, LocalTime to, Set<Long> vehicles) {
        super(id, from, to, vehicles);
        if (minLat >= maxLat || minLon >= maxLon) throw new IllegalArgumentException("invalid rectangle");
        this.box = new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    @Override
    public boolean contains(double lat, double lon) {
        return lat >= box.minLat() && lat <= box.maxLat()
                && lon >= box.minLon() && lon <= box.maxLon();
    }

    @Override
    public BoundingBox boundingBox() { return box; }
}