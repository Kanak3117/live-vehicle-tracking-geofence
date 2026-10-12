package com.kanak.tracking.config;

import com.kanak.tracking.domain.model.*;
import com.kanak.tracking.repository.*;
import com.kanak.tracking.service.GeofenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tracking.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final double ROAD_LON = 77.2090;
    private static final double STOP_LAT = 28.6139;

    private final BusinessUnitRepository businessUnits;
    private final VehicleRepository vehicles;
    private final DeviceRepository devices;
    private final RouteRepository routes;
    private final RoutePointRepository routePoints;
    private final StopRepository stops;
    private final PickupRequestRepository pickups;
    private final GeofenceService geofenceService;

    public DemoDataSeeder(BusinessUnitRepository businessUnits, VehicleRepository vehicles,
                          DeviceRepository devices, RouteRepository routes,
                          RoutePointRepository routePoints, StopRepository stops,
                          PickupRequestRepository pickups, GeofenceService geofenceService) {
        this.businessUnits = businessUnits;
        this.vehicles = vehicles;
        this.devices = devices;
        this.routes = routes;
        this.routePoints = routePoints;
        this.stops = stops;
        this.pickups = pickups;
        this.geofenceService = geofenceService;
    }

    @Override
    public void run(String... args) {
        if (vehicles.count() > 0) {
            return;                                           // already seeded
        }

        BusinessUnit bu = new BusinessUnit();
        bu.setName("Demo Business Unit");
        bu = businessUnits.save(bu);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegNo("DL01AB1234");
        vehicle.setBusinessUnitId(bu.getId());
        vehicle = vehicles.save(vehicle);

        Device device = new Device();
        device.setDeviceId("DEV-001");
        device.setVehicleId(vehicle.getId());
        devices.save(device);

        Route route = new Route();
        route.setVehicleId(vehicle.getId());
        route.setName("Demo Route");
        route = routes.save(route);

        // straight road, 9 points from lat 28.590 to 28.630
        for (int i = 0; i < 9; i++) {
            RoutePoint p = new RoutePoint();
            p.setRouteId(route.getId());
            p.setSeq(i);
            p.setLat(28.590 + i * 0.005);
            p.setLon(ROAD_LON);
            routePoints.save(p);
        }

        Stop stop = new Stop();
        stop.setRouteId(route.getId());
        stop.setSeq(1);
        stop.setName("Employee Pickup Point");
        stop.setLat(STOP_LAT);
        stop.setLon(ROAD_LON);
        stop = stops.save(stop);

        PickupRequest pickup = new PickupRequest();
        pickup.setEmployeeId(1L);
        pickup.setStopId(stop.getId());
        pickup.setVehicleId(vehicle.getId());
        pickup.setActive(true);
        pickups.save(pickup);

        GeofenceEntity zone = new GeofenceEntity();
        zone.setName("Pickup Point 300m");
        zone.setType(GeofenceType.CIRCLE);
        zone.setCenterLat(STOP_LAT);
        zone.setCenterLon(ROAD_LON);
        zone.setRadiusM(300.0);
        geofenceService.create(zone);

        log.info("demo_data_seeded vehicleId={} deviceId=DEV-001 routeId={}", vehicle.getId(), route.getId());
    }
}