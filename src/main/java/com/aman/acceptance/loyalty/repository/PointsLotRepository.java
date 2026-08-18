package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.PointsLot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsLotRepository extends JpaRepository<PointsLot, Long> {
}