package com.kanak.tracking.domain.geofence;

import java.time.LocalTime;
import java.util.Set;

public class CircleGeofence extends Geofence {
    private final double centerLat, centerLon, radiusM;

    public CircleGeofence(Long id, double centerLat, double centerLon, double radiusM,
                          LocalTime from, LocalTime to, Set<Long> vehicles) {
        super(id, from, to, vehicles);
        if (radiusM <= 0) throw new IllegalArgumentException("radius must be > 0");
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.radiusM = radiusM;
    }

    @Override
    public boolean contains(double lat, double lon) {
        return GeoUtils.haversine(centerLat, centerLon, lat, lon) <= radiusM;
    }

    @Override
    public BoundingBox boundingBox() {
        double dLat = radiusM / GeoUtils.METERS_PER_DEG_LAT;
        double dLon = radiusM / (GeoUtils.METERS_PER_DEG_LAT * Math.cos(Math.toRadians(centerLat)));
        return new BoundingBox(centerLat - dLat, centerLon - dLon, centerLat + dLat, centerLon + dLon);
    }
}