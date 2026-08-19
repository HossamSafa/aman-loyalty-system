package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RedeemMode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class RedemptionCalculationService {

    @Value("${loyalty.rules.default-redemption-rate:0.005}")
    private BigDecimal conversionRate;

    public CalculationResult calculateDiscount(BigDecimal purchaseAmount, long requestedPoints, RedeemMode redeemMode, long totalAvailablePoints) {
        log.info("[LOYALTY] START RedemptionCalculationService.calculateDiscount | requestedPoints={} | redeemMode={}", requestedPoints, redeemMode);
        long actualPointsToRedeem = 0;
        
        if (redeemMode == RedeemMode.FULL) {
            BigDecimal requiredPointsDecimal = purchaseAmount.divide(conversionRate, RoundingMode.CEILING);
            long requiredPoints = requiredPointsDecimal.longValue();
            actualPointsToRedeem = Math.min(totalAvailablePoints, requiredPoints);
        } else if (redeemMode == RedeemMode.PARTIAL) {
            if (requestedPoints > totalAvailablePoints) {
                log.warn("[LOYALTY] Calculation failed - insufficient points | requested={} | totalAvailable={}", requestedPoints, totalAvailablePoints);
                throw LoyaltyException.unprocessable(ErrorCode.LOYALTY_INSUFFICIENT_AVAILABLE_POINTS,"Requested points exceed total available points");
            }
            actualPointsToRedeem = requestedPoints;
        }
        
        BigDecimal discountAmount = BigDecimal.valueOf(actualPointsToRedeem).multiply(conversionRate);
        
        if (discountAmount.compareTo(purchaseAmount) > 0) {
            discountAmount = purchaseAmount;
        }
        
        BigDecimal payableAfterDiscount = purchaseAmount.subtract(discountAmount);
        
        log.info("[LOYALTY] Redemption calculated | actualPoints={} | discountAmount={}", actualPointsToRedeem, discountAmount);
        log.info("[LOYALTY] END RedemptionCalculationService.calculateDiscount");
        return new CalculationResult(actualPointsToRedeem, discountAmount, payableAfterDiscount);
    }

    public record CalculationResult(long actualPointsToRedeem, BigDecimal discountAmount, BigDecimal payableAfterDiscount) {}
}
