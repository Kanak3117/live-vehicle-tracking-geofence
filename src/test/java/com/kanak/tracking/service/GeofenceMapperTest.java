package com.kanak.tracking.service;

import com.kanak.tracking.domain.geofence.CircleGeofence;
import com.kanak.tracking.domain.geofence.Geofence;
import com.kanak.tracking.domain.geofence.RectGeofence;
import com.kanak.tracking.domain.model.GeofenceEntity;
import com.kanak.tracking.domain.model.GeofenceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeofenceMapperTest {

    private final GeofenceMapper mapper = new GeofenceMapper();

    @Test
    void circleEntityBecomesCircleGeofence() {
        GeofenceEntity e = new GeofenceEntity();
        e.setId(1L);
        e.setType(GeofenceType.CIRCLE);
        e.setCenterLat(28.6139);
        e.setCenterLon(77.2090);
        e.setRadiusM(300.0);

        Geofence g = mapper.toDomain(e);
        assertInstanceOf(CircleGeofence.class, g);
        assertTrue(g.contains(28.6139, 77.2090));
        assertTrue(g.appliesTo(42L));                      // no vehicle list = all vehicles
    }

    @Test
    void rectEntityBecomesRectGeofence() {
        GeofenceEntity e = new GeofenceEntity();
        e.setId(2L);
        e.setType(GeofenceType.RECT);
        e.setMinLat(28.60);
        e.setMinLon(77.20);
        e.setMaxLat(28.62);
        e.setMaxLon(77.22);

        assertInstanceOf(RectGeofence.class, mapper.toDomain(e));
    }

    @Test
    void missingFieldsAreRejected() {
        GeofenceEntity e = new GeofenceEntity();
        e.setType(GeofenceType.CIRCLE);                    // no centre, no radius
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(e));
    }

    @Test
    void missingTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(new GeofenceEntity()));
    }
}