package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    Optional<LoyaltyTransaction> findBySourceTransactionId(String sourceTransactionId);
    Optional<LoyaltyTransaction> findByIdempotencyKey(String idempotencyKey);
}
