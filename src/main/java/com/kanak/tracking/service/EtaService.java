package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.PickupRequest;
import com.kanak.tracking.domain.model.Route;
import com.kanak.tracking.domain.model.RoutePoint;
import com.kanak.tracking.domain.model.Stop;
import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.eta.RouteSnapper;
import com.kanak.tracking.eta.SpeedEstimator;
import com.kanak.tracking.repository.PickupRequestRepository;
import com.kanak.tracking.repository.RoutePointRepository;
import com.kanak.tracking.repository.RouteRepository;
import com.kanak.tracking.repository.StopRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EtaService {

    /** If the stop is more than this far behind the cab, we treat it as already passed. */
    private static final double PASSED_TOLERANCE_M = 50.0;

    public record PickupEta(Long pickupRequestId, Long employeeId, Long stopId, String stopName,
                            double remainingMeters, long etaSeconds, boolean passed) {}

    public record VehicleEta(long vehicleId, Instant basedOnFixTime,
                             double offRouteMeters, List<PickupEta> pickups) {}

    private final LiveLocationStore store;
    private final SpeedEstimator speedEstimator;
    private final RouteRepository routeRepository;
    private final RoutePointRepository routePointRepository;
    private final StopRepository stopRepository;
    private final PickupRequestRepository pickupRepository;

    private final ConcurrentHashMap<Long, RouteSnapper> snappers = new ConcurrentHashMap<>();   // by routeId
    private final ConcurrentHashMap<Long, VehicleEta> etaCache = new ConcurrentHashMap<>();     // by vehicleId

    public EtaService(LiveLocationStore store, SpeedEstimator speedEstimator,
                      RouteRepository routeRepository, RoutePointRepository routePointRepository,
                      StopRepository stopRepository, PickupRequestRepository pickupRepository) {
        this.store = store;
        this.speedEstimator = speedEstimator;
        this.routeRepository = routeRepository;
        this.routePointRepository = routePointRepository;
        this.stopRepository = stopRepository;
        this.pickupRepository = pickupRepository;
    }

    public VehicleEta getEta(long vehicleId) {
        LocationFix fix = store.get(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("No live location yet for vehicle " + vehicleId));

        VehicleEta cached = etaCache.get(vehicleId);
        if (cached != null && cached.basedOnFixTime().equals(fix.timestamp())) {
            return cached;                                   // no new ping since last time
        }

        Route route = routeRepository.findFirstByVehicleId(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("No route assigned to vehicle " + vehicleId));
        RouteSnapper snapper = snappers.computeIfAbsent(route.getId(), this::buildSnapper);

        RouteSnapper.SnapResult vehicleOnRoute = snapper.snap(fix.lat(), fix.lon());
        List<PickupEta> result = new ArrayList<>();

        for (PickupRequest p : pickupRepository.findByVehicleIdAndActiveTrue(vehicleId)) {
            Stop stop = stopRepository.findById(p.getStopId()).orElse(null);
            if (stop == null) continue;

            double remaining = snapper.snap(stop.getLat(), stop.getLon()).distanceAlongRouteM()
                    - vehicleOnRoute.distanceAlongRouteM();
            boolean passed = remaining < -PASSED_TOLERANCE_M;
            long eta = passed ? 0 : Math.round(speedEstimator.etaSeconds(vehicleId, Math.max(0, remaining)));

            result.add(new PickupEta(p.getId(), p.getEmployeeId(), stop.getId(), stop.getName(),
                    Math.max(0, remaining), eta, passed));
        }
        result.sort(Comparator.comparingDouble(PickupEta::remainingMeters));

        VehicleEta fresh = new VehicleEta(vehicleId, fix.timestamp(), vehicleOnRoute.offRouteM(), result);
        etaCache.put(vehicleId, fresh);
        return fresh;
    }

    private RouteSnapper buildSnapper(Long routeId) {
        List<RoutePoint> pts = routePointRepository.findByRouteIdOrderBySeqAsc(routeId);
        List<RouteSnapper.Point> points = pts.stream()
                .map(rp -> new RouteSnapper.Point(rp.getLat(), rp.getLon()))
                .toList();
        return new RouteSnapper(points);                     // throws if fewer than 2 points
    }
}