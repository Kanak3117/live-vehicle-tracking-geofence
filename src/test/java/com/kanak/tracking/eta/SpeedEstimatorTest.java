package com.kanak.tracking.eta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedEstimatorTest {

    @Test
    void firstReadingIsTakenAsIs() {
        SpeedEstimator s = new SpeedEstimator(0.3, 5, 20, 1.0);
        assertEquals(40, s.update(1L, 40), 0.001);
    }

    @Test
    void laterReadingsAreSmoothed() {
        SpeedEstimator s = new SpeedEstimator(0.3, 5, 20, 1.0);
        s.update(1L, 20);
        assertEquals(26, s.update(1L, 40), 0.001);               // 0.3*40 + 0.7*20
    }

    @Test
    void stoppedVehicleUsesMinimumSpeedFloor() {
        SpeedEstimator s = new SpeedEstimator(0.3, 5, 20, 1.0);
        s.update(1L, 0);
        assertEquals(5 / 3.6, s.effectiveMetersPerSecond(1L), 0.001);
    }

    @Test
    void unknownVehicleUsesDefaultSpeed() {
        SpeedEstimator s = new SpeedEstimator(0.3, 5, 20, 1.0);
        assertEquals(20 / 3.6, s.effectiveMetersPerSecond(99L), 0.001);
    }

    @Test
    void trafficFactorSlowsTheEta() {
        SpeedEstimator free = new SpeedEstimator(0.3, 5, 20, 1.0);
        SpeedEstimator jam = new SpeedEstimator(0.3, 5, 20, 0.5);
        assertEquals(2 * free.etaSeconds(1L, 1000), jam.etaSeconds(1L, 1000), 0.001);
    }

    @Test
    void negativeReadingIsIgnored() {
        SpeedEstimator s = new SpeedEstimator(0.3, 5, 20, 1.0);
        s.update(1L, 30);
        assertEquals(30, s.update(1L, -5), 0.001);
    }

    @Test
    void reachedStopHasZeroEta() {
        assertEquals(0, new SpeedEstimator(0.3, 5, 20, 1.0).etaSeconds(1L, -50), 0.001);
    }
}
