package com.kanak.tracking.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(
        columnNames = {"geofence_id", "vehicle_id", "event_time", "event_type"}))
public class GeofenceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "geofence_id")
    private Long geofenceId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType type;

    @Column(name = "event_time")
    private Instant eventTime;

    private double lat;
    private double lon;
}