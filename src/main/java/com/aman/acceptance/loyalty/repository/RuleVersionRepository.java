package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.RuleStatus;
import com.aman.acceptance.loyalty.model.RuleVersion;
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
    public interface RuleVersionRepository
            extends JpaRepository<RuleVersion, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
       SELECT r
       FROM RuleVersion r
       WHERE r.program.id = :programId
         AND r.effectiveFrom <= :time
         AND (r.effectiveTo IS NULL OR r.effectiveTo > :time)
       ORDER BY r.version DESC
       """)
        Optional<RuleVersion> findEffectiveRuleForUpdate(
                @Param("programId") Long programId,
                @Param("time") LocalDateTime time
        );
        Optional<RuleVersion> findTopByProgramIdOrderByVersionDesc(Long programId);


        List<RuleVersion> findByProgramId(Long programId);

        List<RuleVersion> findByStatusAndEffectiveFromLessThanEqual(
                RuleStatus status,
                LocalDateTime time
        );
        List<RuleVersion> findByStatusAndEffectiveToLessThanEqual(
                RuleStatus status,
                LocalDateTime time
        );
    }
