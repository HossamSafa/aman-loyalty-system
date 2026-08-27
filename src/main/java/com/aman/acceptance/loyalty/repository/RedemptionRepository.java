package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.model.Redemption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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


    @Query("""
    SELECT COALESCE(SUM(r.discountAmount), 0)
    FROM Redemption r
    WHERE r.account.program.id = :programId
      AND r.status = com.aman.acceptance.loyalty.enums.RedemptionStatus.COMMITTED
      AND r.createdAt >= :from
      AND r.createdAt <= :to
    """)
    BigDecimal sumCommittedRedemptionValue(
            @Param("programId") Long programId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COUNT(r)
    FROM Redemption r
    WHERE r.account.program.id = :programId
      AND r.status = com.aman.acceptance.loyalty.enums.RedemptionStatus.COMMITTED
      AND r.createdAt >= :from
      AND r.createdAt <= :to
    """)
    Long countCommittedRedemptions(
            @Param("programId") Long programId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}