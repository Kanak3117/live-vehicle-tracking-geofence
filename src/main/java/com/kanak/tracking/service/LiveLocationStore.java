package com.kanak.tracking.service;

import com.kanak.tracking.domain.state.LocationFix;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveLocationStore {

    public enum Outcome { UPDATED, DUPLICATE, OUT_OF_ORDER }

    private final ConcurrentHashMap<Long, LocationFix> latest = new ConcurrentHashMap<>();

    /** Atomic per vehicle: check and update happen together, so two threads can't race. */
    public Outcome offer(LocationFix fix) {
        Outcome[] outcome = new Outcome[1];
        latest.compute(fix.vehicleId(), (id, current) -> {
            if (current == null || fix.timestamp().isAfter(current.timestamp())) {
                outcome[0] = Outcome.UPDATED;
                return fix;
            }
            outcome[0] = fix.timestamp().equals(current.timestamp())
                    ? Outcome.DUPLICATE
                    : Outcome.OUT_OF_ORDER;
            return current;
        });
        return outcome[0];
    }

    public Optional<LocationFix> get(long vehicleId) {
        return Optional.ofNullable(latest.get(vehicleId));
    }
}