package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM LoyaltyAccount a
            WHERE a.id = :id
            """)
    Optional<LoyaltyAccount> findByIdForUpdate(@Param("id") Long id);
}