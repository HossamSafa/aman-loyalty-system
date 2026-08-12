package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.PointsLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsLotRepository extends JpaRepository<PointsLot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM PointsLot p
        WHERE p.account.id = :accountId
          AND p.status = com.aman.acceptance.loyalty.enums.LotStatus.AVAILABLE
          AND p.remainingPoints > 0
          AND p.expiresAt > :now
        ORDER BY p.expiresAt ASC, p.id ASC
        """)
    List<PointsLot> findAvailableLotsForRedemption(
            @Param("accountId") Long accountId,
            @Param("now") LocalDateTime now
    );
}