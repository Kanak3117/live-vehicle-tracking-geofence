package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    List<RoutePoint> findByRouteIdOrderBySeqAsc(Long routeId);
}