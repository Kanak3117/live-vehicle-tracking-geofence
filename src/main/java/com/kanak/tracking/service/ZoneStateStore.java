package com.kanak.tracking.service;

import com.kanak.tracking.domain.state.ZoneState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ZoneStateStore {

    /** Per-vehicle zone status. Callers synchronize on the VehicleZones object. */
    public static class VehicleZones {
        private boolean initialized;
        private final Map<Long, ZoneState> states = new HashMap<>();

        public boolean isInitialized() { return initialized; }
        public void markInitialized() { this.initialized = true; }

        public ZoneState get(long zoneId) {
            return states.getOrDefault(zoneId, ZoneState.OUTSIDE);   // unseen zone = outside
        }

        public void set(long zoneId, ZoneState s) { states.put(zoneId, s); }
        public void remove(long zoneId) { states.remove(zoneId); }

        public Set<Long> insideZoneIds() {
            return states.entrySet().stream()
                    .filter(e -> e.getValue() == ZoneState.INSIDE)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }

    private final ConcurrentHashMap<Long, VehicleZones> byVehicle = new ConcurrentHashMap<>();

    public VehicleZones forVehicle(long vehicleId) {
        return byVehicle.computeIfAbsent(vehicleId, id -> new VehicleZones());
    }
}