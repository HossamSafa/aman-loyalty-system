package com.aman.acceptance.loyalty.repository;

import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.model.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    List<Redemption> findByStatusAndCreatedAtBefore(RedemptionStatus status, LocalDateTime cutoffTime);
}
