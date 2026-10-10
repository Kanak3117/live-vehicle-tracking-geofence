package com.kanak.tracking.index;

import com.kanak.tracking.domain.geofence.CircleGeofence;
import com.kanak.tracking.domain.geofence.Geofence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpatialGridIndexTest {

    private static final double LAT = 28.6139, LON = 77.2090;

    private SpatialGridIndex index;
    private Geofence zone;

    @BeforeEach
    void setUp() {
        index = new SpatialGridIndex();
        zone = new CircleGeofence(1L, LAT, LON, 300, null, null, Set.of());
        index.add(zone);
    }

    @Test
    void findsZoneNearby() {
        assertEquals(1, index.candidatesNear(LAT, LON).size());
    }

    @Test
    void doesNotReturnZoneFarAway() {
        assertTrue(index.candidatesNear(LAT + 0.05, LON).isEmpty());
    }

    @Test
    void zoneSpanningTwoCellsIsFoundFromBothSides() {
        // lon 77.2090 is just before the 77.21 cell boundary; ~200 m east is in the next cell
        List<Geofence> east = index.candidatesNear(LAT, 77.2110);
        assertEquals(1, east.size());
        assertEquals(1, index.candidatesNear(LAT, 77.2085).size());
    }

    @Test
    void removedZoneIsNoLongerReturned() {
        index.remove(zone);
        assertTrue(index.candidatesNear(LAT, LON).isEmpty());
        assertTrue(index.candidatesNear(LAT, 77.2110).isEmpty());
    }

    @Test
    void manyZonesStillOnlyReturnNearbyOnes() {
        for (int i = 0; i < 1000; i++) {
            index.add(new CircleGeofence(100L + i, 12.0 + i * 0.02, 75.0, 300, null, null, Set.of()));
        }
        assertEquals(1, index.candidatesNear(LAT, LON).size());   // not 1001
    }
}