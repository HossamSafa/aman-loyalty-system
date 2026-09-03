package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyProgram;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByProgramAndCustomer(LoyaltyProgram program, Customer customer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM LoyaltyAccount account
            WHERE account.id = :accountId
            """)
    Optional<LoyaltyAccount> findByIdForUpdate(@Param("accountId") Long accountId);
    @Query("SELECT a FROM LoyaltyAccount a WHERE a.id = :id")
    Optional<LoyaltyAccount> findByIdWithLock( Long id);

    @Query("SELECT SUM(a.availablePoints), SUM(a.lockedPoints), SUM(a.reservedPoints) " +
            "FROM LoyaltyAccount a")
    List<Object[]> getBalanceTotals();

    @EntityGraph(attributePaths = {"program", "customer"})
    Page<LoyaltyAccount> findAll(Pageable pageable);

    @Query("""
    SELECT COUNT(a)
    FROM LoyaltyAccount a
    WHERE a.program.id = :programId
      AND a.status = com.aman.acceptance.loyalty.enums.AccountStatus.ACTIVE
    """)
    Long countActiveCustomers(
            @Param("programId") Long programId
    );

    @Query("""
    SELECT COUNT(a)
    FROM LoyaltyAccount a
    WHERE a.program.id = :programId
      AND a.createdAt >= :from
      AND a.createdAt <= :to
    """)
    Long countNewCustomers(
            @Param("programId") Long programId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}
