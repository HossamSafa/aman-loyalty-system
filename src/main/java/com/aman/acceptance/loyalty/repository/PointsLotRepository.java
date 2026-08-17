package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.PointsLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

//public interface PointsLotRepository  extends JpaRepository<PointsLot, Long> {
//    Optional<PointsLot> findByEarningTransactionId(Long earningTransactionId);
//}
public interface PointsLotRepository
        extends JpaRepository<PointsLot, Long> {
    Optional<PointsLot>
    findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
            Long accountId,
            LotStatus status,
            Integer remainingPoints);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot
            FROM PointsLot lot
            WHERE lot.earningTransaction.id = :earningTransactionId
            """)

    Optional<PointsLot> findByEarningTransactionIdForUpdate(
            @Param("earningTransactionId")
            Long earningTransactionId
    );
}