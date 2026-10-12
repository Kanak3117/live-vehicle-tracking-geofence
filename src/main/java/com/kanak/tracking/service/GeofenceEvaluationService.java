package com.kanak.tracking.service;

import com.kanak.tracking.domain.geofence.Geofence;
import com.kanak.tracking.domain.model.EventType;
import com.kanak.tracking.domain.model.GeofenceEvent;
import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.domain.state.ZoneState;
import com.kanak.tracking.index.SpatialGridIndex;
import com.kanak.tracking.repository.GeofenceEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeofenceEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceEvaluationService.class);

    private final SpatialGridIndex index;
    private final ZoneStateStore stateStore;
    private final GeofenceEventRepository eventRepository;
    private final TrackingMetrics metrics;
    private final ZoneId zone;

    public GeofenceEvaluationService(SpatialGridIndex index,
                                     ZoneStateStore stateStore,
                                     GeofenceEventRepository eventRepository,
                                     TrackingMetrics metrics,
                                     @Value("${tracking.timezone:Asia/Kolkata}") String timezone) {
        this.index = index;
        this.stateStore = stateStore;
        this.eventRepository = eventRepository;
        this.metrics = metrics;
        this.zone = ZoneId.of(timezone);
    }

    public List<GeofenceEvent> evaluate(LocationFix fix) {
        List<GeofenceEvent> fired = new ArrayList<>();
        LocalTime localTime = fix.timestamp().atZone(zone).toLocalTime();
        ZoneStateStore.VehicleZones vz = stateStore.forVehicle(fix.vehicleId());

        synchronized (vz) {
            boolean firstPing = !vz.isInitialized();
            vz.markInitialized();

            Map<Long, Geofence> toCheck = new LinkedHashMap<>();
            for (Geofence g : index.candidatesNear(fix.lat(), fix.lon())) {
                toCheck.put(g.getId(), g);
            }
            for (Long zoneId : vz.insideZoneIds()) {
                Geofence g = index.findById(zoneId);
                if (g == null) {
                    vz.remove(zoneId);
                } else {
                    toCheck.put(zoneId, g);
                }
            }

            for (Geofence g : toCheck.values()) {
                if (!g.appliesTo(fix.vehicleId())) continue;

                ZoneState before = vz.get(g.getId());
                ZoneState now = g.contains(fix.lat(), fix.lon()) ? ZoneState.INSIDE : ZoneState.OUTSIDE;
                vz.set(g.getId(), now);

                if (firstPing) continue;
                if (!g.isActiveAt(localTime)) continue;
                if (before == now) continue;

                EventType type = (now == ZoneState.INSIDE) ? EventType.ENTRY : EventType.EXIT;
                fired.add(buildEvent(g, fix, type));
            }
        }

        if (!fired.isEmpty()) {
            eventRepository.saveAll(fired);
            fired.forEach(e -> {
                metrics.geofenceEvent(e.getType());
                log.info("geofence_event type={} vehicleId={} geofenceId={}",
                        e.getType(), e.getVehicleId(), e.getGeofenceId());
            });
        }
        return fired;
    }

    private GeofenceEvent buildEvent(Geofence g, LocationFix fix, EventType type) {
        GeofenceEvent e = new GeofenceEvent();
        e.setGeofenceId(g.getId());
        e.setVehicleId(fix.vehicleId());
        e.setType(type);
        e.setEventTime(fix.timestamp());
        e.setLat(fix.lat());
        e.setLon(fix.lon());
        return e;
    }
}