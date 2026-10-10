package com.kanak.tracking.repository;

import com.kanak.tracking.domain.model.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
}