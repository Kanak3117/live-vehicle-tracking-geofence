package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.PickupRequest;
import com.kanak.tracking.domain.model.Route;
import com.kanak.tracking.domain.model.RoutePoint;
import com.kanak.tracking.domain.model.Stop;
import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.eta.SpeedEstimator;
import com.kanak.tracking.repository.PickupRequestRepository;
import com.kanak.tracking.repository.RoutePointRepository;
import com.kanak.tracking.repository.RouteRepository;
import com.kanak.tracking.repository.StopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EtaServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-10T06:30:00Z");
    private static final double LON = 77.20;

    private LiveLocationStore store;
    private SpeedEstimator speed;
    private RouteRepository routeRepo;
    private RoutePointRepository pointRepo;
    private StopRepository stopRepo;
    private PickupRequestRepository pickupRepo;
    private EtaService service;

    @BeforeEach
    void setUp() {
        store = new LiveLocationStore();
        speed = new SpeedEstimator(0.3, 5, 20, 1.0);
        routeRepo = mock(RouteRepository.class);
        pointRepo = mock(RoutePointRepository.class);
        stopRepo = mock(StopRepository.class);
        pickupRepo = mock(PickupRequestRepository.class);

        Route route = new Route();
        route.setId(10L);
        route.setVehicleId(7L);
        when(routeRepo.findFirstByVehicleId(7L)).thenReturn(Optional.of(route));
        // straight north-south road, about 2.2 km
        when(pointRepo.findByRouteIdOrderBySeqAsc(10L))
                .thenReturn(List.of(point(1, 28.60), point(2, 28.62)));

        service = new EtaService(store, speed, routeRepo, pointRepo, stopRepo, pickupRepo);
    }

    private RoutePoint point(int seq, double lat) {
        RoutePoint p = new RoutePoint();
        p.setRouteId(10L);
        p.setSeq(seq);
        p.setLat(lat);
        p.setLon(LON);
        return p;
    }

    private void givenPickupAt(double stopLat) {
        PickupRequest p = new PickupRequest();
        p.setId(1L);
        p.setEmployeeId(5L);
        p.setStopId(100L);
        p.setVehicleId(7L);
        Stop s = new Stop();
        s.setId(100L);
        s.setName("Main Gate");
        s.setLat(stopLat);
        s.setLon(LON);
        when(pickupRepo.findByVehicleIdAndActiveTrue(7L)).thenReturn(List.of(p));
        when(stopRepo.findById(100L)).thenReturn(Optional.of(s));
    }

    private void ping(double lat, Instant t) {
        store.offer(new LocationFix(7L, lat, LON, 0, 36, t));
        speed.update(7L, 36);                                 // 36 km/h = 10 m/s
    }

    @Test
    void etaToStopAhead() {
        givenPickupAt(28.615);
        ping(28.605, T0);                                     // ~1112 m behind the stop

        EtaService.PickupEta eta = service.getEta(7L).pickups().get(0);
        assertFalse(eta.passed());
        assertEquals(1112, eta.remainingMeters(), 15);
        assertEquals(111, eta.etaSeconds(), 3);               // 1112 m / 10 m/s
    }

    @Test
    void stopAlreadyPassedHasZeroEta() {
        givenPickupAt(28.605);
        ping(28.615, T0);                                     // cab is past the stop

        EtaService.PickupEta eta = service.getEta(7L).pickups().get(0);
        assertTrue(eta.passed());
        assertEquals(0, eta.etaSeconds());
    }

    @Test
    void noLiveLocationThrows() {
        assertThrows(NoSuchElementException.class, () -> service.getEta(7L));
    }

    @Test
    void vehicleWithoutRouteThrows() {
        when(routeRepo.findFirstByVehicleId(9L)).thenReturn(Optional.empty());
        store.offer(new LocationFix(9L, 28.605, LON, 0, 36, T0));
        assertThrows(NoSuchElementException.class, () -> service.getEta(9L));
    }

    @Test
    void etaIsRecomputedOnlyWhenANewFixArrives() {
        givenPickupAt(28.615);
        ping(28.605, T0);

        service.getEta(7L);
        service.getEta(7L);
        verify(pickupRepo, times(1)).findByVehicleIdAndActiveTrue(7L);   // second call came from cache

        ping(28.606, T0.plusSeconds(5));
        service.getEta(7L);
        verify(pickupRepo, times(2)).findByVehicleIdAndActiveTrue(7L);   // new fix, recomputed
    }
}