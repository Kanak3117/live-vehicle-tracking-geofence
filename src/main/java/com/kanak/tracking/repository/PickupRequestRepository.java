package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.PickupRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {
    List<PickupRequest> findByVehicleIdAndActiveTrue(Long vehicleId);
}