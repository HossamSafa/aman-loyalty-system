package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.TransactionStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoyaltyTransactionRepository
        extends JpaRepository<LoyaltyTransaction, Long> {

    Page<LoyaltyTransaction> findByAccount_Id(Long accountId, Pageable pageable);

    Optional<LoyaltyTransaction> findBySourceTransactionIdAndType(
            String sourceTransactionId,
            TransactionType type
    );

    List<LoyaltyTransaction> findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(
            Long accountId,
            String originalSourceTransactionId,
            TransactionType type,
            TransactionStatus status
    );

    Optional<LoyaltyTransaction> findBySourceTransactionId(String sourceTransactionId);

    Optional<LoyaltyTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<LoyaltyTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Using exists() followed by save() is not a real protection against concurrent requests.
    // We need to read the existing operation and return the result of the retry instead.
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS month,
            SUM(CASE WHEN type = 'EARN' THEN points ELSE 0 END) AS issued,
            SUM(CASE WHEN type = 'REDEEM' THEN ABS(points) ELSE 0 END) AS redeemed
        FROM loyalty_transactions
        WHERE created_at >= :startDate
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY DATE_TRUNC('month', created_at)
        """, nativeQuery = true)
    List<Object[]> getPointsFlowByMonth(@Param("startDate") LocalDateTime startDate);
}