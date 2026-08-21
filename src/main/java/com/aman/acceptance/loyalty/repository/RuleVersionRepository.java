package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.model.RuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    @Query("""
            SELECT r
            FROM RuleVersion r
            WHERE r.program.id = :programId
              AND r.status = :status
              AND r.effectiveFrom <= :transactionTime
              AND (r.effectiveTo IS NULL OR r.effectiveTo > :transactionTime)
            """)
    List<RuleVersion> findEffectiveRules(
            @Param("programId") Long programId,
            @Param("transactionTime") LocalDateTime transactionTime,
            @Param("status") RuleStatus status
    );
}
