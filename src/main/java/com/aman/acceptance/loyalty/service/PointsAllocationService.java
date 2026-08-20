package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.RedemptionAllocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PointsAllocationService {

    public List<RedemptionAllocation> allocate(List<PointsLot> availableLots, long pointsToReserve) {
        log.info("[LOYALTY] START PointsAllocationService.allocate | requestedPoints={}", pointsToReserve);
        List<RedemptionAllocation> allocations = new ArrayList<>();
        long remainingToReserve = pointsToReserve;
        
        for (PointsLot lot : availableLots) {
            if (remainingToReserve <= 0) break;

            int toDeduct = Math.toIntExact(Math.min(lot.getRemainingPoints(), remainingToReserve));
            lot.setRemainingPoints(lot.getRemainingPoints() - toDeduct);
            remainingToReserve -= toDeduct;

            RedemptionAllocation allocation = RedemptionAllocation.builder()
                    .lot(lot)
                    .points(toDeduct)
                    .build();
            
            allocations.add(allocation);
            log.info("[LOYALTY] Allocating lot | lotId={} | available={} | allocated={}", lot.getId(), lot.getRemainingPoints() + toDeduct, toDeduct);
        }

        if (remainingToReserve > 0) {
            log.warn("[LOYALTY] Allocation failed - insufficient points | requested={} | remaining={}", pointsToReserve, remainingToReserve);
            throw  LoyaltyException.unprocessable(ErrorCode.LOYALTY_INSUFFICIENT_AVAILABLE_POINTS,"Not enough points in available lots");
        }
        
        log.info("[LOYALTY] Allocation completed | requested={} | allocated={}", pointsToReserve, pointsToReserve);
        log.info("[LOYALTY] END PointsAllocationService.allocate");
        return allocations;
    }
}
