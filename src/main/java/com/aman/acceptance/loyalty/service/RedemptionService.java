package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionMoneyDto;
import com.aman.acceptance.loyalty.model.dto.response.VerifyRedemptionResponseData;
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

import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.model.dto.request.CommitRequest;
import com.aman.acceptance.loyalty.model.dto.response.CommitResponseData;
import com.aman.acceptance.loyalty.model.dto.response.BalanceDto;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.enums.TransactionStatus;

@RequiredArgsConstructor
@Service
public class RedemptionService {

        private final LoyaltyAccountRepository accountRepository;
        private final PointsLotRepository pointsLotRepository;
        private final RedemptionRepository redemptionRepository;
        private final LoyaltyTransactionRepository loyaltyTransactionRepository;
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

                // Get the loyalty account and lock it for this transaction
                LoyaltyAccount account = accountRepository.findByIdWithLock(accountId)
                                .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                                "No loyalty account exists with id: " + accountId));

                // Check Points Lot
                List<PointsLot> availableLots = pointsLotRepository.findAvailableLotsForRedemption(accountId,
                                LocalDateTime.now());

                // Calculate the total available points
                long totalAvailablePoints = availableLots.stream()
                                .mapToLong(PointsLot::getRemainingPoints)
                                .sum();

                // Calculate (purchaseAmount, requestedPoints and totalAvailablePoints) and
                // select redeemMode
                RedemptionCalculationService.CalculationResult calcResult = calculationService.calculateDiscount(
                                request.purchaseAmount().value(),
                                request.requestedPoints(),
                                request.redeemMode(),
                                totalAvailablePoints);

                // Get the actual number of points to redeem
                long actualPointsToRedeem = calcResult.actualPointsToRedeem();
                // Convert points to int safely because requestedPoints are Integer
                int pointsToRedeemInt = Math.toIntExact(actualPointsToRedeem);

                // Create a new redemption with OTP_PENDING status
                Redemption redemption = Redemption.builder()
                                .account(account)
                                .purchaseTransactionId(request.purchaseTransactionId())
                                .requestedPoints(pointsToRedeemInt)
                                .discountAmount(calcResult.discountAmount())
                                .status(RedemptionStatus.OTP_PENDING)
                                .build();
                // Allocate the points across the available lots
                List<RedemptionAllocation> allocations = allocationService.allocate(availableLots,
                                actualPointsToRedeem);

                // Add the allocations to the redemption
                allocations.forEach(redemption::addAllocation);

                // Move points from available to reserved
                account.setAvailablePoints(account.getAvailablePoints() - pointsToRedeemInt);
                account.setReservedPoints(account.getReservedPoints() + pointsToRedeemInt);

                accountRepository.save(account);
                pointsLotRepository.saveAll(availableLots);

                // Initiate OTP (mutates redemption with otpCode, expiry, attempts)
                OtpMetadataDto otpMetadata = otpService.initiate(account, redemption);

                redemptionRepository.save(redemption);

                // Return the redemption details to the client
                return new RedemptionResponseData(
                                redemption.getId(),
                                redemption.getStatus(),
                                actualPointsToRedeem,
                                new RedemptionMoneyDto(calcResult.discountAmount(),
                                                request.purchaseAmount().currency()),
                                new RedemptionMoneyDto(calcResult.payableAfterDiscount(),
                                                request.purchaseAmount().currency()),
                                otpMetadata);
        }

        @Transactional
        public VerifyRedemptionResponseData verifyRedemption(Long id, String otp) {
                Redemption redemption = redemptionRepository.findByIdWithLock(id)
                                .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                                "Redemption not found with id: " + id));

                if (redemption.getStatus() != RedemptionStatus.OTP_PENDING) {
                        throw LoyaltyException.conflict(ErrorCode.LOYALTY_REDEMPTION_STATE_CONFLICT,
                                        "Redemption is not in OTP_PENDING state. Current state: "
                                                        + redemption.getStatus());
                }

                otpService.verifyOtp(redemption, otp);

                String authCode = otpService.generateAuthorizationCode();
                redemption.setStatus(RedemptionStatus.AUTHORIZED);
                redemption.setAuthorizationCode(authCode);
                redemption.setOtpCode(null);

                redemptionRepository.save(redemption);

                return new VerifyRedemptionResponseData(
                                redemption.getId(),
                                redemption.getStatus(),
                                authCode,
                                (long) redemption.getRequestedPoints(),
                                new RedemptionMoneyDto(redemption.getDiscountAmount(), "EGP"),
                                redemption.getReservationExpiresAt());
        }

        @Transactional
        public CommitResponseData commitRedemption(Long id, CommitRequest request) {
                Redemption redemption = redemptionRepository.findByIdWithLock(id)
                                .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                                "Redemption not found with id: " + id));

                LoyaltyAccount account = redemption.getAccount();
                String idempKey = "RED_COMMIT_" + redemption.getId();

                if (redemption.getStatus() == RedemptionStatus.COMMITTED) {
                        LoyaltyTransaction existingTx = loyaltyTransactionRepository.findByIdempotencyKey(idempKey)
                                        .orElseThrow(() -> LoyaltyException.conflict(ErrorCode.INTERNAL_SERVER_ERROR, "Transaction not found for committed redemption"));
                        return buildCommitResponse(redemption, existingTx, account);
                }

                if (redemption.getStatus() != RedemptionStatus.AUTHORIZED) {
                        throw LoyaltyException.badRequest(ErrorCode.LOYALTY_REDEMPTION_STATE_CONFLICT,
                                        "Redemption is not in AUTHORIZED state. Current state: "
                                                        + redemption.getStatus());
                }

                if (!request.authorizationCode().equals(redemption.getAuthorizationCode())) {
                        throw LoyaltyException.badRequest(ErrorCode.LOYALTY_OTP_INVALID, "Invalid authorization code");
                }

                // Finalize Reservation
                int points = redemption.getRequestedPoints();
                account.setReservedPoints(account.getReservedPoints() - points);
                accountRepository.save(account);

                redemption.setStatus(RedemptionStatus.COMMITTED);
                redemptionRepository.save(redemption);

                LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                                .account(account)
                                .type(TransactionType.REDEEM)
                                .sourceTransactionId(redemption.getPurchaseTransactionId())
                                .points(points)
                                .moneyAmount(redemption.getDiscountAmount())
                                .status(TransactionStatus.COMMITTED)
                                .idempotencyKey(idempKey)
                                .build();

                loyaltyTransactionRepository.save(transaction);

                return buildCommitResponse(redemption, transaction, account);
        }

        private CommitResponseData buildCommitResponse(Redemption redemption, LoyaltyTransaction transaction, LoyaltyAccount account) {
                BalanceDto balance = new BalanceDto(
                                account.getAvailablePoints(),
                                account.getLockedPoints(),
                                account.getReservedPoints(),
                                account.getTotalOwned()
                );

                return new CommitResponseData(
                                "red-" + redemption.getId(),
                                redemption.getStatus(),
                                (long) redemption.getRequestedPoints(),
                                new RedemptionMoneyDto(redemption.getDiscountAmount(), "EGP"),
                                "ltx-" + transaction.getId(),
                                balance
                );
        }
}

