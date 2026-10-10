package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.GeofenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceRepository extends JpaRepository<GeofenceEntity, Long> {
}