package com.kanak.tracking.index;

import com.kanak.tracking.domain.geofence.BoundingBox;
import com.kanak.tracking.domain.geofence.Geofence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;

@Component
public class SpatialGridIndex {

    private static final double CELL_DEG = 0.01;   // ~1.1 km per cell

    private final ConcurrentHashMap<Long, Set<Geofence>> cells = new ConcurrentHashMap<>();

    public void add(Geofence g) {
        forEachCell(g.boundingBox(), key ->
                cells.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(g));
    }

    public void remove(Geofence g) {
        forEachCell(g.boundingBox(), key -> {
            Set<Geofence> set = cells.get(key);
            if (set != null) {
                set.removeIf(x -> Objects.equals(x.getId(), g.getId()));
            }
        });
    }

    /** O(1) cell lookup + O(k) for the k zones registered in that cell. */
    public List<Geofence> candidatesNear(double lat, double lon) {
        Set<Geofence> set = cells.get(key(cell(lat), cell(lon)));
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public void clear() {
        cells.clear();
    }

    private void forEachCell(BoundingBox b, LongConsumer action) {
        long r0 = cell(b.minLat()), r1 = cell(b.maxLat());
        long c0 = cell(b.minLon()), c1 = cell(b.maxLon());
        for (long r = r0; r <= r1; r++) {
            for (long c = c0; c <= c1; c++) {
                action.accept(key(r, c));
            }
        }
    }

    private static long cell(double degrees) {
        return (long) Math.floor(degrees / CELL_DEG);
    }

    private static long key(long row, long col) {
        return (row << 32) ^ (col & 0xFFFFFFFFL);
    }
}