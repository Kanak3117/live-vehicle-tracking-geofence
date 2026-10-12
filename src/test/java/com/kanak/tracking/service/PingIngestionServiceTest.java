package com.kanak.tracking.service;

import com.kanak.tracking.eta.SpeedEstimator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PingIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
    private static final double LAT = 28.6139, LON = 77.2090;

    private LiveLocationStore store;
    private SimpleMeterRegistry registry;
    private PingIngestionService service;

    @BeforeEach
    void setUp() {
        DeviceLookupService lookup = mock(DeviceLookupService.class);
        when(lookup.findVehicleId("D1")).thenReturn(7L);
        when(lookup.findVehicleId("UNKNOWN")).thenReturn(null);

        store = new LiveLocationStore();
        registry = new SimpleMeterRegistry();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PingIngestionService(lookup, store,
                mock(GeofenceEvaluationService.class),
                new SpeedEstimator(0.3, 5, 20, 1.0),
                new TrackingMetrics(registry), clock, 300);
    }

    private double count(String result) {
        return registry.counter("tracking.pings", "result", result).count();
    }

    @Test
    void firstPingIsAccepted() {
        assertEquals(PingResult.ACCEPTED, service.ingest("D1", LAT, LON, 0, 10, NOW));
        assertTrue(store.get(7L).isPresent());
    }

    @Test
    void sameTimestampIsDuplicate() {
        service.ingest("D1", LAT, LON, 0, 10, NOW);
        assertEquals(PingResult.DUPLICATE, service.ingest("D1", LAT, LON, 0, 10, NOW));
    }

    @Test
    void olderPingNeverOverwritesNewerFix() {
        service.ingest("D1", 28.70, 77.30, 0, 10, NOW);
        PingResult r = service.ingest("D1", 28.10, 77.10, 0, 10, NOW.minusSeconds(180));
        assertEquals(PingResult.OUT_OF_ORDER, r);
        assertEquals(28.70, store.get(7L).get().lat());
    }

    @Test
    void newerPingReplacesOlder() {
        service.ingest("D1", 28.10, 77.10, 0, 10, NOW.minusSeconds(10));
        assertEquals(PingResult.ACCEPTED, service.ingest("D1", 28.20, 77.20, 0, 10, NOW));
        assertEquals(28.20, store.get(7L).get().lat());
    }

    @Test
    void pingBeyondStalenessLimitIsDropped() {
        assertEquals(PingResult.STALE, service.ingest("D1", LAT, LON, 0, 10, NOW.minusSeconds(301)));
        assertTrue(store.get(7L).isEmpty());
    }

    @Test
    void unknownDeviceIsRejected() {
        assertEquals(PingResult.UNKNOWN_DEVICE, service.ingest("UNKNOWN", LAT, LON, 0, 10, NOW));
    }

    @Test
    void invalidCoordinatesThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.ingest("D1", 95, LON, 0, 10, NOW));
    }

    @Test
    void countersTrackEveryOutcome() {
        service.ingest("D1", LAT, LON, 0, 10, NOW);                           // accepted
        service.ingest("D1", LAT, LON, 0, 10, NOW);                           // duplicate
        service.ingest("D1", LAT, LON, 0, 10, NOW.minusSeconds(10));          // out of order
        service.ingest("D1", LAT, LON, 0, 10, NOW.minusSeconds(400));         // stale

        assertEquals(1, count("ACCEPTED"));
        assertEquals(1, count("DUPLICATE"));
        assertEquals(1, count("OUT_OF_ORDER"));
        assertEquals(1, count("STALE"));
    }
}