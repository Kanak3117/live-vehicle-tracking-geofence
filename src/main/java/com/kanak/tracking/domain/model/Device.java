package com.kanak.tracking.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Device {
    @Id
    private String deviceId;

    @Column(nullable = false)
    private Long vehicleId;
}