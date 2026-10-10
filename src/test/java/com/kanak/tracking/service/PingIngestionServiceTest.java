package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.Device;
import com.kanak.tracking.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.kanak.tracking.eta.SpeedEstimator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PingIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
    private static final double LAT = 28.6139, LON = 77.2090;

    private LiveLocationStore store;
    private PingIngestionService service;

    @BeforeEach
    void setUp() {
        DeviceRepository repo = mock(DeviceRepository.class);
        Device device = new Device();
        device.setDeviceId("D1");
        device.setVehicleId(7L);
        when(repo.findById("D1")).thenReturn(Optional.of(device));
        when(repo.findById("UNKNOWN")).thenReturn(Optional.empty());

        store = new LiveLocationStore();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PingIngestionService(repo, store,
                mock(GeofenceEvaluationService.class),
                new SpeedEstimator(0.3, 5, 20, 1.0),
                clock, 300);
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
        service.ingest("D1", 28.70, 77.30, 0, 10, NOW);                       // fresh
        PingResult r = service.ingest("D1", 28.10, 77.10, 0, 10, NOW.minusSeconds(180)); // 3 min old
        assertEquals(PingResult.OUT_OF_ORDER, r);
        assertEquals(28.70, store.get(7L).get().lat());                       // newer stands
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
}