package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findFirstByVehicleId(Long vehicleId);
}