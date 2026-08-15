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

    List<Redemption> findByStatusAndCreatedAtBefore(RedemptionStatus status, LocalDateTime cutoffTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Redemption r WHERE r.id = :id")
    Optional<Redemption> findByIdWithLock(@Param("id") Long id);
}
