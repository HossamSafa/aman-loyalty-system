package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.RedemptionAllocation;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PointsAllocationService {

    public List<RedemptionAllocation> allocate(List<PointsLot> availableLots, long pointsToReserve) {
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
        }

        if (remainingToReserve > 0) {
            throw  LoyaltyException.unprocessable(ErrorCode.LOYALTY_INSUFFICIENT_AVAILABLE_POINTS,"Not enough points in available lots");
        }
        
        return allocations;
    }
}
