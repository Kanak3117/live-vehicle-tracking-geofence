package com.kanak.tracking.service;

import com.kanak.tracking.domain.geofence.CircleGeofence;
import com.kanak.tracking.domain.model.EventType;
import com.kanak.tracking.domain.model.GeofenceEvent;
import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.index.SpatialGridIndex;
import com.kanak.tracking.repository.GeofenceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GeofenceEvaluationServiceTest {

    private static final double LAT = 28.6139, LON = 77.2090;      // zone centre, radius 300 m
    private static final double FAR_LAT = 28.6247;                  // ~1.2 km away
    private static final Instant T0 = Instant.parse("2026-10-10T06:30:00Z");   // 12:00 IST

    private SpatialGridIndex index;
    private GeofenceEvaluationService service;

    @BeforeEach
    void setUp() {
        index = new SpatialGridIndex();
        service = new GeofenceEvaluationService(index, new ZoneStateStore(),
                mock(GeofenceEventRepository.class), "Asia/Kolkata");
        index.add(new CircleGeofence(1L, LAT, LON, 300, null, null, Set.of()));
    }

    private LocationFix fix(long vehicleId, double lat, Instant t) {
        return new LocationFix(vehicleId, lat, LON, 0, 10, t);
    }

    @Test
    void entryFiresOnceThenExitFiresOnce() {
        assertTrue(service.evaluate(fix(7, FAR_LAT, T0)).isEmpty());                 // 1.2 km away

        List<GeofenceEvent> in = service.evaluate(fix(7, LAT + 0.0018, T0.plusSeconds(5)));   // ~200 m
        assertEquals(1, in.size());
        assertEquals(EventType.ENTRY, in.get(0).getType());

        assertTrue(service.evaluate(fix(7, LAT + 0.0010, T0.plusSeconds(10))).isEmpty());     // still inside
        assertTrue(service.evaluate(fix(7, LAT, T0.plusSeconds(15))).isEmpty());              // still inside

        List<GeofenceEvent> out = service.evaluate(fix(7, 28.70, T0.plusSeconds(20)));        // far away
        assertEquals(1, out.size());
        assertEquals(EventType.EXIT, out.get(0).getType());

        assertTrue(service.evaluate(fix(7, 28.71, T0.plusSeconds(25))).isEmpty());            // no repeat EXIT
    }

    @Test
    void firstPingInsideDoesNotFireEntry() {
        assertTrue(service.evaluate(fix(7, LAT, T0)).isEmpty());
    }

    @Test
    void gapWithoutCrossingFiresNothing() {
        service.evaluate(fix(7, FAR_LAT, T0));
        assertTrue(service.evaluate(fix(7, FAR_LAT + 0.0005, T0.plusSeconds(120))).isEmpty());  // dark 2 min, still outside
    }

    @Test
    void gapWithRealCrossingFiresExactlyOneEntry() {
        service.evaluate(fix(7, FAR_LAT, T0));
        List<GeofenceEvent> e = service.evaluate(fix(7, LAT, T0.plusSeconds(120)));   // reappears inside
        assertEquals(1, e.size());
        assertEquals(EventType.ENTRY, e.get(0).getType());
        assertTrue(service.evaluate(fix(7, LAT, T0.plusSeconds(125))).isEmpty());     // not again
    }

    @Test
    void zoneOutsideItsTimeWindowIgnoresCrossings() {
        index.clear();
        index.add(new CircleGeofence(2L, LAT, LON, 300, LocalTime.of(21, 0), LocalTime.of(5, 0), Set.of()));

        service.evaluate(fix(7, FAR_LAT, T0));                                                 // 12:00 IST
        assertTrue(service.evaluate(fix(7, LAT, T0.plusSeconds(5))).isEmpty());                // inactive window

        Instant night = Instant.parse("2026-10-10T17:00:00Z");                                 // 22:30 IST
        service.evaluate(fix(8, FAR_LAT, night));
        assertEquals(1, service.evaluate(fix(8, LAT, night.plusSeconds(5))).size());           // active window
    }

    @Test
    void deviceSpecificZoneIgnoresOtherVehicles() {
        index.clear();
        index.add(new CircleGeofence(3L, LAT, LON, 300, null, null, Set.of(99L)));

        service.evaluate(fix(7, FAR_LAT, T0));
        assertTrue(service.evaluate(fix(7, LAT, T0.plusSeconds(5))).isEmpty());                // vehicle 7 not allowed

        service.evaluate(fix(99, FAR_LAT, T0));
        assertEquals(1, service.evaluate(fix(99, LAT, T0.plusSeconds(5))).size());             // vehicle 99 allowed
    }
}