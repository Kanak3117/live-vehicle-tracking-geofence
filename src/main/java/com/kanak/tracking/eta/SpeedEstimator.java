package com.kanak.tracking.eta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpeedEstimator {

    private final double alpha;            // weight of the newest reading (0..1)
    private final double minKmh;           // floor, so a red light doesn't give infinite ETA
    private final double defaultKmh;       // used before any reading exists
    private final double trafficFactor;    // 1.0 = free flow, 0.8 = 20% slower
    private final ConcurrentHashMap<Long, Double> smoothedKmh = new ConcurrentHashMap<>();

    public SpeedEstimator(@Value("${tracking.eta.alpha:0.3}") double alpha,
                          @Value("${tracking.eta.min-kmh:5}") double minKmh,
                          @Value("${tracking.eta.default-kmh:20}") double defaultKmh,
                          @Value("${tracking.eta.traffic-factor:1.0}") double trafficFactor) {
        this.alpha = alpha;
        this.minKmh = minKmh;
        this.defaultKmh = defaultKmh;
        this.trafficFactor = trafficFactor;
    }

    /** Exponentially weighted moving average, kept per vehicle. */
    public double update(long vehicleId, double speedKmh) {
        if (Double.isNaN(speedKmh) || speedKmh < 0) {
            return smoothedKmh.getOrDefault(vehicleId, defaultKmh);
        }
        return smoothedKmh.merge(vehicleId, speedKmh,
                (old, fresh) -> alpha * fresh + (1 - alpha) * old);
    }

    public double effectiveMetersPerSecond(long vehicleId) {
        double kmh = Math.max(smoothedKmh.getOrDefault(vehicleId, defaultKmh), minKmh);
        return kmh * trafficFactor / 3.6;
    }

    public double etaSeconds(long vehicleId, double remainingMeters) {
        if (remainingMeters <= 0) return 0;
        return remainingMeters / effectiveMetersPerSecond(vehicleId);
    }
}