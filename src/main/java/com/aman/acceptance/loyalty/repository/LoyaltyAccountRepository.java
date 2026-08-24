package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM LoyaltyAccount account
            WHERE account.id = :accountId
            """)
    Optional<LoyaltyAccount> findByIdForUpdate(@Param("accountId") Long accountId);

    @Query("SELECT a FROM LoyaltyAccount a WHERE a.id = :id")
    Optional<LoyaltyAccount> findByIdWithLock(Long id);

    Optional<LoyaltyAccount> findByProgramAndCustomer(
            LoyaltyProgram program,
            Customer customer
    );

    boolean existsById(Long id);
}