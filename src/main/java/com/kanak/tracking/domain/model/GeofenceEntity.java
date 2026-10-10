package com.kanak.tracking.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class GeofenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private GeofenceType type;

    // circle fields
    private Double centerLat;
    private Double centerLon;
    private Double radiusM;

    // rectangle fields
    private Double minLat;
    private Double minLon;
    private Double maxLat;
    private Double maxLon;

    // time window (null = always active)
    private LocalTime activeFrom;
    private LocalTime activeTo;

    // empty = applies to all vehicles
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Long> allowedVehicleIds = new HashSet<>();
}