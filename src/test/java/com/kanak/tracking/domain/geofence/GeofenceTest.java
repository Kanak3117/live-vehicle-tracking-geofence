package com.kanak.tracking.domain.geofence;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GeofenceTest {
    private static final double LAT = 28.6139, LON = 77.2090;   // ~Delhi
    // 0.0018 deg lat ≈ 200 m, 0.0036 ≈ 400 m

    private CircleGeofence circle() {
        return new CircleGeofence(1L, LAT, LON, 300, null, null, Set.of());
    }

    @Test void circle_insideAndOutside() {
        assertTrue(circle().contains(LAT + 0.0018, LON));    // ~200 m
        assertFalse(circle().contains(LAT + 0.0036, LON));   // ~400 m
        assertTrue(circle().contains(LAT, LON));             // centre
    }

    @Test void rect_insideOutsideBoundary() {
        RectGeofence r = new RectGeofence(2L, 28.60, 77.20, 28.62, 77.22, null, null, Set.of());
        assertTrue(r.contains(28.61, 77.21));
        assertTrue(r.contains(28.60, 77.20));      // boundary counts as inside
        assertFalse(r.contains(28.63, 77.21));
    }

    @Test void timeWindow_normal() {
        Geofence g = new CircleGeofence(3L, LAT, LON, 300, LocalTime.of(9, 0), LocalTime.of(18, 0), Set.of());
        assertTrue(g.isActiveAt(LocalTime.of(12, 0)));
        assertFalse(g.isActiveAt(LocalTime.of(20, 0)));
    }

    @Test void timeWindow_wrapsMidnight() {
        Geofence g = new CircleGeofence(4L, LAT, LON, 300, LocalTime.of(21, 0), LocalTime.of(5, 0), Set.of());
        assertTrue(g.isActiveAt(LocalTime.of(23, 0)));
        assertTrue(g.isActiveAt(LocalTime.of(3, 0)));
        assertFalse(g.isActiveAt(LocalTime.of(12, 0)));
    }

    @Test void deviceSpecific() {
        Geofence g = new CircleGeofence(5L, LAT, LON, 300, null, null, Set.of(7L));
        assertTrue(g.appliesTo(7L));
        assertFalse(g.appliesTo(8L));
        assertTrue(circle().appliesTo(99L));       // empty set = all vehicles
    }

    @Test void invalidShapesRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CircleGeofence(6L, LAT, LON, 0, null, null, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new RectGeofence(7L, 28.62, 77.20, 28.60, 77.22, null, null, Set.of()));
    }
}