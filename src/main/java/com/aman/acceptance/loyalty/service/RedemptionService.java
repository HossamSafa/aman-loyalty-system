package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.enums.RedemptionCancelReason;
import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.enums.TransactionStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.exception.OtpInvalidException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.model.RedemptionAllocation;
import com.aman.acceptance.loyalty.model.dto.request.CancelRequest;
import com.aman.acceptance.loyalty.model.dto.request.CommitRequest;
import com.aman.acceptance.loyalty.model.dto.request.RedemptionRequest;
import com.aman.acceptance.loyalty.model.dto.response.CancelResponseData;
import com.aman.acceptance.loyalty.model.dto.response.CommitResponseData;
import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionMoneyDto;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionResponseData;
import com.aman.acceptance.loyalty.model.dto.response.VerifyRedemptionResponseData;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import com.aman.acceptance.loyalty.service.mapper.RedemptionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RedemptionService {

        private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);

        private final LoyaltyAccountRepository accountRepository;
        private final PointsLotRepository pointsLotRepository;
        private final RedemptionRepository redemptionRepository;
        private final LoyaltyTransactionRepository loyaltyTransactionRepository;
        private final RedemptionCalculationService calculationService;
        private final PointsAllocationService allocationService;
        private final OtpService otpService;
        private final RedemptionResponseMapper responseMapper;

        @Transactional
        public RedemptionResponseData initiateRedemption(RedemptionRequest request) {
                Long accountId = request.accountId();
                if (accountId == null) {
                        throw new IllegalArgumentException("Account ID cannot be null");
                }

                LoyaltyAccount account = accountRepository.findByIdWithLock(accountId)
                        .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                "No loyalty account exists with id: " + accountId));

                List<PointsLot> availableLots = pointsLotRepository.findAvailableLotsForRedemption(accountId, LocalDateTime.now());

                long totalAvailablePoints = availableLots.stream()
                        .mapToLong(PointsLot::getRemainingPoints)
                        .sum();

                RedemptionCalculationService.CalculationResult calcResult = calculationService.calculateDiscount(
                        request.purchaseAmount().value(),
                        request.requestedPoints(),
                        request.redeemMode(),
                        totalAvailablePoints);

                int pointsToRedeemInt = Math.toIntExact(calcResult.actualPointsToRedeem());

                Redemption redemption = Redemption.builder()
                        .account(account)
                        .purchaseTransactionId(request.purchaseTransactionId())
                        .requestedPoints(pointsToRedeemInt)
                        .discountAmount(calcResult.discountAmount())
                        .status(RedemptionStatus.OTP_PENDING)
                        .build();

                List<RedemptionAllocation> allocations = allocationService.allocate(availableLots, pointsToRedeemInt);
                allocations.forEach(redemption::addAllocation);

                account.reserveForRedemption(pointsToRedeemInt);

                OtpMetadataDto otpMetadata = otpService.initiate(account, redemption);

                redemptionRepository.save(redemption);

                return new RedemptionResponseData(
                        redemption.getId(),
                        redemption.getStatus(),
                        (long) pointsToRedeemInt,
                        new RedemptionMoneyDto(calcResult.discountAmount(), request.purchaseAmount().currency()),
                        new RedemptionMoneyDto(calcResult.payableAfterDiscount(), request.purchaseAmount().currency()),
                        otpMetadata);
        }

        @Transactional(noRollbackFor = OtpInvalidException.class)
        public VerifyRedemptionResponseData verifyRedemption(Long id, String otp) {
                Redemption redemption = getRedemptionOrThrow(id);

                redemption.assertStatus(RedemptionStatus.OTP_PENDING);
                otpService.verifyOtp(redemption, otp);

                String authCode = otpService.generateAuthorizationCode();
                redemption.authorize(authCode, RESERVATION_TTL);

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
                Redemption redemption = getRedemptionOrThrow(id);
                LoyaltyAccount account = redemption.getAccount();
                String idempKey = "RED_COMMIT_" + redemption.getId();

                if (redemption.getStatus() == RedemptionStatus.COMMITTED) {
                        LoyaltyTransaction existingTx = loyaltyTransactionRepository.findByIdempotencyKey(idempKey)
                                .orElseThrow(() -> LoyaltyException.conflict(ErrorCode.INTERNAL_SERVER_ERROR,
                                        "Transaction not found for committed redemption"));
                        return responseMapper.mapToCommitResponse(redemption, existingTx, account);
                }

                redemption.assertStatus(RedemptionStatus.AUTHORIZED);

                if (!redemption.isAuthorizedWith(request.authorizationCode())) {
                        throw LoyaltyException.badRequest(ErrorCode.LOYALTY_OTP_INVALID, "Invalid authorization code");
                }

                int points = redemption.getRequestedPoints();
                account.finalizeReservation(points);
                redemption.commit();

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

                return responseMapper.mapToCommitResponse(redemption, transaction, account);
        }

        @Transactional
        public CancelResponseData cancelRedemption(Long id, CancelRequest request) {
                return cancelRedemptionInternal(id, request.reason());
        }

        @Transactional
        public CancelResponseData cancelRedemptionInternal(Long id, RedemptionCancelReason reason) {
                Redemption redemption = getRedemptionOrThrow(id);
                LoyaltyAccount account = redemption.getAccount();

                if (redemption.getStatus() == RedemptionStatus.COMMITTED) {
                        throw LoyaltyException.conflict(ErrorCode.LOYALTY_REDEMPTION_STATE_CONFLICT,
                                "Redemption is already COMMITTED, cannot cancel.");
                }

                if (redemption.getStatus() == RedemptionStatus.CANCELLED) {
                        return responseMapper.mapToCancelResponse(redemption, account);
                }

                redemption.getAllocations().forEach(allocation ->
                        allocation.getLot().restore(allocation.getPoints()));

                int points = redemption.getRequestedPoints();
                account.releaseReservation(points);

                redemption.cancel(reason);

                return responseMapper.mapToCancelResponse(redemption, account);
        }

        private Redemption getRedemptionOrThrow(Long id) {
                return redemptionRepository.findByIdWithLock(id)
                        .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                "Redemption not found with id: " + id));
        }
}