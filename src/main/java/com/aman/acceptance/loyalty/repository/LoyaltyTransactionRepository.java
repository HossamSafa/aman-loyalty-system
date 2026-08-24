package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.TransactionStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTransactionRepository
        extends JpaRepository<LoyaltyTransaction, Long> {

    Page<LoyaltyTransaction> findByAccount_Id(Long accountId, Pageable pageable);

    Optional<LoyaltyTransaction> findBySourceTransactionIdAndType(String sourceTransactionId, TransactionType type);

    List<LoyaltyTransaction>
    findAllByAccount_IdAndOriginalSourceTransactionIdAndTypeAndStatus(Long accountId, String originalSourceTransactionId, TransactionType type, TransactionStatus status);

    boolean existsByAccount_Program_IdAndSourceTransactionIdAndType(
            Long programId,
            String sourceTransactionId,
            TransactionType type
    );

    Optional<LoyaltyTransaction> findBySourceTransactionId(String sourceTransactionId);

    Optional<LoyaltyTransaction> findByIdempotencyKey(String idempotencyKey);

    //Using exists() followed by save() is not a real protection against concurrent requests. We need to read the existing operation and return the result of the retry instead.
}