package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.model.Redemption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    Optional<Redemption> findByIdAndAccount_Id(Long id, Long accountId);
    List<Redemption> findByStatusAndCreatedAtBefore(RedemptionStatus status, LocalDateTime cutoffTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Redemption r WHERE r.id = :id")
    Optional<Redemption> findByIdWithLock(@Param("id") Long id);

    List<Redemption> findByStatusAndOtpExpiresAtBefore(RedemptionStatus status, LocalDateTime time);

    List<Redemption> findByStatusAndReservationExpiresAtBefore(RedemptionStatus status, LocalDateTime time);

    boolean existsByPurchaseTransactionId(String purchaseTransactionId);

    @Query(value = """
        SELECT
            COUNT(*) AS reserved,
            SUM(CASE WHEN status IN ('AUTHORIZED', 'COMMITTED') THEN 1 ELSE 0 END) AS verified,
            SUM(CASE WHEN status = 'COMMITTED' THEN 1 ELSE 0 END) AS committed
        FROM redemptions
        WHERE created_at >= :startDate
        """, nativeQuery = true)
    List<Object[]> getOtpFunnelCounts(@Param("startDate") LocalDateTime startDate);

    List<Redemption> findByAccount_IdAndStatusIn(Long accountId, List<RedemptionStatus> statuses);

}