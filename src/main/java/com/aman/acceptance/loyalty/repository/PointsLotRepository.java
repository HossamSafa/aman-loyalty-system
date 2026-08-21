package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.PointsLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointsLotRepository extends JpaRepository<PointsLot, Long> {

    Optional<PointsLot>
    findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
            Long accountId, LotStatus status, Integer remainingPoints);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot
            FROM PointsLot lot
            WHERE lot.earningTransaction.id = :earningTransactionId
            """)
    Optional<PointsLot> findByEarningTransactionIdForUpdate(
            @Param("earningTransactionId") Long earningTransactionId);

    List<PointsLot> findByStatusAndUnlockAtLessThanEqualAndRemainingPointsGreaterThan(
            Pageable page, LotStatus status, LocalDateTime unlockAt, Integer remainingPoints);

//    List<PointsLot>
//    findByStatusAndExpiresAtLessThanEqualAndRemainingPointsGreaterThanOrderByExpiresAtAscIdAsc(
//            Pageable page, LotStatus status, LocalDateTime expiresAt, Integer remainingPoints);

    List<PointsLot>
    findByStatusAndExpiresAtLessThanEqualAndRemainingPointsGreaterThanAndIdGreaterThanOrderByIdAsc(
            LotStatus status,LocalDateTime expiresAt, Integer remainingPoints, Long checkpointId, Pageable page);

    @Query(value = """
            SELECT *
            FROM points_lots
            WHERE status = :status
              AND expires_at <= :expiresAt
              AND remaining_points > :remainingPoints
              AND id > :checkpointId
            ORDER BY id ASC
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PointsLot> findExpiredLotsForUpdate(
            @Param("status") String status,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("remainingPoints") Integer remainingPoints,
            @Param("checkpointId") Long checkpointId,
            Pageable pageable
    );
}