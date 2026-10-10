package com.kanak.tracking.eta;

import com.kanak.tracking.eta.RouteSnapper.Point;
import com.kanak.tracking.eta.RouteSnapper.SnapResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteSnapperTest {

    // straight north-south road, about 2.2 km long
    private final RouteSnapper straight = new RouteSnapper(List.of(
            new Point(28.60, 77.20), new Point(28.62, 77.20)));

    @Test
    void snapsToMiddleOfRoad() {
        SnapResult r = straight.snap(28.61, 77.2005);           // ~50 m east of the road
        assertEquals(straight.totalLengthM() / 2, r.distanceAlongRouteM(), 10);
        assertTrue(r.offRouteM() > 40 && r.offRouteM() < 60);
    }

    @Test
    void pointBeforeStartClampsToStart() {
        assertEquals(0, straight.snap(28.59, 77.20).distanceAlongRouteM(), 0.001);
    }

    @Test
    void pointAfterEndClampsToEnd() {
        assertEquals(straight.totalLengthM(), straight.snap(28.63, 77.20).distanceAlongRouteM(), 0.001);
    }

    @Test
    void remainingDistanceToStopAhead() {
        double remaining = straight.remainingToM(28.605, 77.20, 28.615, 77.20);
        assertEquals(1112, remaining, 15);                       // 0.01 deg of latitude
    }

    @Test
    void stopAlreadyPassedIsNegative() {
        assertTrue(straight.remainingToM(28.615, 77.20, 28.605, 77.20) < 0);
    }

    @Test
    void picksNearestSegmentOnBentRoute() {
        RouteSnapper bent = new RouteSnapper(List.of(
                new Point(28.60, 77.20), new Point(28.61, 77.20), new Point(28.61, 77.21)));
        assertEquals(1, bent.snap(28.6101, 77.205).segmentIndex());
        assertEquals(0, bent.snap(28.605, 77.2001).segmentIndex());
    }

    @Test
    void routeNeedsAtLeastTwoPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteSnapper(List.of(new Point(28.6, 77.2))));
    }
}