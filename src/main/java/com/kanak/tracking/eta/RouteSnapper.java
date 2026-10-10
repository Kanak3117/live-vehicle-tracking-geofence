package com.kanak.tracking.eta;

import com.kanak.tracking.domain.geofence.GeoUtils;

import java.util.List;

public class RouteSnapper {

    private static final double M_PER_DEG = 111_320.0;

    public record Point(double lat, double lon) {}

    public record SnapResult(int segmentIndex, double snappedLat, double snappedLon,
                             double distanceAlongRouteM, double offRouteM) {}

    private final List<Point> points;
    private final double[] cumulative;     // cumulative[i] = metres from route start to point i

    public RouteSnapper(List<Point> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("Route needs at least 2 points");
        }
        this.points = List.copyOf(points);
        this.cumulative = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1);
            Point b = points.get(i);
            cumulative[i] = cumulative[i - 1] + GeoUtils.haversine(a.lat(), a.lon(), b.lat(), b.lon());
        }
    }

    public double totalLengthM() {
        return cumulative[cumulative.length - 1];
    }

    /** Projects the point onto the nearest route segment. O(n) in route points. */
    public SnapResult snap(double lat, double lon) {
        SnapResult best = null;
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);

            // small-area flat projection in metres, centred on point a
            double cos = Math.cos(Math.toRadians(a.lat()));
            double bx = (b.lon() - a.lon()) * cos * M_PER_DEG;
            double by = (b.lat() - a.lat()) * M_PER_DEG;
            double px = (lon - a.lon()) * cos * M_PER_DEG;
            double py = (lat - a.lat()) * M_PER_DEG;

            double len2 = bx * bx + by * by;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / len2));
            double off = Math.hypot(px - t * bx, py - t * by);

            if (best == null || off < best.offRouteM()) {
                double segLen = cumulative[i + 1] - cumulative[i];
                best = new SnapResult(i,
                        a.lat() + t * (b.lat() - a.lat()),
                        a.lon() + t * (b.lon() - a.lon()),
                        cumulative[i] + t * segLen,
                        off);
            }
        }
        return best;
    }

    /** Metres still to travel along the route to reach the stop. Negative = stop already passed. */
    public double remainingToM(double vehLat, double vehLon, double stopLat, double stopLon) {
        return snap(stopLat, stopLon).distanceAlongRouteM()
                - snap(vehLat, vehLon).distanceAlongRouteM();
    }
}