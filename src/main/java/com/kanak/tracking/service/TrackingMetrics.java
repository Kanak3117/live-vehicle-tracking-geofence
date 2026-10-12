package com.kanak.tracking.service;

import com.kanak.tracking.domain.model.EventType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TrackingMetrics {

    private final MeterRegistry registry;

    public TrackingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void ping(PingResult result) {
        registry.counter("tracking.pings", "result", result.name()).increment();
    }

    public void geofenceEvent(EventType type) {
        registry.counter("tracking.geofence.events", "type", type.name()).increment();
    }

    public void ingestLatency(long nanos) {
        registry.timer("tracking.ping.latency").record(nanos, TimeUnit.NANOSECONDS);
    }
}