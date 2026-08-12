package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionMoneyDto;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.dto.request.RedemptionRequest;
import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.model.RedemptionAllocation;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionResponseData;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RedemptionService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointsLotRepository pointsLotRepository;
    private final RedemptionRepository redemptionRepository;
    private final RedemptionCalculationService calculationService;
    private final PointsAllocationService allocationService;
    private final OtpService otpService;



    @Transactional
    public RedemptionResponseData initiateRedemption(RedemptionRequest request) {
        // Checks Account Availability
        Long accountId = request.accountId();
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        //Get the loyalty account and lock it for this transaction
        LoyaltyAccount account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                        "No loyalty account exists with id: " + accountId));

        // Check Points Lot
        List<PointsLot> availableLots = pointsLotRepository.findAvailableLotsForRedemption(accountId, LocalDateTime.now());

        //Calculate the total available points
        long totalAvailablePoints = availableLots.stream()
                .mapToLong(PointsLot::getRemainingPoints)
                .sum();

        // Calculate (purchaseAmount, requestedPoints and totalAvailablePoints) and select redeemMode
        RedemptionCalculationService.CalculationResult calcResult = calculationService.calculateDiscount(
                request.purchaseAmount().value(),
                request.requestedPoints(),
                request.redeemMode(),
                totalAvailablePoints
        );

        //Get the actual number of points to redeem
        long actualPointsToRedeem = calcResult.actualPointsToRedeem();
        // Convert points to int safely because requestedPoints are Integer
        int pointsToRedeemInt = Math.toIntExact(actualPointsToRedeem);

        //Create a new redemption with OTP_PENDING status
        Redemption redemption = Redemption.builder()
                .account(account)
                .purchaseTransactionId(request.purchaseTransactionId())
                .requestedPoints(pointsToRedeemInt)
                .discountAmount(calcResult.discountAmount())
                .status(RedemptionStatus.OTP_PENDING)
                .build();
        //Allocate the points across the available lots
        List<RedemptionAllocation> allocations = allocationService.allocate(availableLots, actualPointsToRedeem);

        //Add the allocations to the redemption
        allocations.forEach(redemption::addAllocation);

        //Move points from available to reserved
        account.setAvailablePoints(account.getAvailablePoints() - pointsToRedeemInt);
        account.setReservedPoints(account.getReservedPoints() + pointsToRedeemInt);

        accountRepository.save(account);
        pointsLotRepository.saveAll(availableLots);
        redemptionRepository.save(redemption);

        // Initiate otp service
        OtpMetadataDto otpMetadata = otpService.initiate(account, redemption);

        //Return the redemption details to the client
        return new RedemptionResponseData(
                redemption.getId(),
                redemption.getStatus(),
                actualPointsToRedeem,
                new RedemptionMoneyDto(calcResult.discountAmount(), request.purchaseAmount().currency()),
                new RedemptionMoneyDto(calcResult.payableAfterDiscount(), request.purchaseAmount().currency()),
                otpMetadata
        );
    }
}
