package com.aman.acceptance.loyalty.repositries;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.model.PointsLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsLotRepository  extends JpaRepository<PointsLot, Long> {
    Optional<PointsLot>
    findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc(
            Long accountId,
            LotStatus status,
            Integer remainingPoints);

    Optional<PointsLot>
    findFirstByAccount_IdAndStatusAndRemainingPointsLessThanOrderByExpiresAtAscUnlockAtAsc(
            Long accountId,
            LotStatus status,
            Integer remainingPoints);

}
