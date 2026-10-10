package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StopRepository extends JpaRepository<Stop, Long> {
    List<Stop> findByRouteIdOrderBySeqAsc(Long routeId);
}