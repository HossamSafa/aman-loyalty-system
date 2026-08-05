package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.PointsLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsLotRepository  extends JpaRepository<PointsLot, Long> {
    Optional<PointsLot> findByEarningTransactionId(Long earningTransactionId);
}
