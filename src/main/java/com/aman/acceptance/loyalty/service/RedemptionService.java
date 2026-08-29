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
import com.aman.acceptance.loyalty.model.dto.response.*;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import com.aman.acceptance.loyalty.service.mapper.RedemptionResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
        private final AccountStatusGuard accountStatusGuard;


        @Transactional
        public RedemptionResponseData initiateRedemption(RedemptionRequest request) {
                Long accountId = request.accountId();
                if (accountId == null) {
                        throw new IllegalArgumentException("Account ID cannot be null");
                }

                if (redemptionRepository.existsByPurchaseTransactionId(request.purchaseTransactionId())) {
                        throw LoyaltyException.conflict(
                                ErrorCode.DUPLICATE_PURCHASE_TRANSACTION_ID,
                                "A redemption already exists for purchase transaction id: "
                                        + request.purchaseTransactionId()
                        );
                }

                LoyaltyAccount account = accountRepository.findByIdWithLock(accountId)
                        .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                "No loyalty account exists with id: " + accountId));
                log.info("[LOYALTY] Account found | accountId={}", accountId);

            accountStatusGuard.assertActive(account);
            log.info("[LOYALTY] Account status validated as ACTIVE | accountId={}", accountId);


            List<PointsLot> availableLots = pointsLotRepository.findAvailableLotsForRedemption(accountId, LocalDateTime.now());

                long totalAvailablePoints = availableLots.stream()
                        .mapToLong(PointsLot::getRemainingPoints)
                        .sum();
                log.info("[LOYALTY] Available lots found | count={} | totalPoints={}", availableLots.size(), totalAvailablePoints);

                RedemptionCalculationService.CalculationResult calcResult = calculationService.calculateDiscount(
                        request.purchaseAmount().value(),
                        request.requestedPoints(),
                        request.redeemMode(),
                        totalAvailablePoints);
                log.info("[LOYALTY] Redemption calculated | points={} | discount={}", calcResult.actualPointsToRedeem(), calcResult.discountAmount());

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
                log.info("[LOYALTY] Points allocated | allocations={}", allocations.size());

                account.reserveForRedemption(pointsToRedeemInt);
                log.info("[LOYALTY] Points reserved | accountId={} | points={}", accountId, pointsToRedeemInt);

                OtpMetadataDto otpMetadata = otpService.initiate(account, redemption);
                log.info("[LOYALTY] OTP initiated | redemptionId={}", redemption.getId());

                redemptionRepository.save(redemption);
                log.info("[LOYALTY] Redemption saved | redemptionId={} | status={}", redemption.getId(), redemption.getStatus());
                log.info("[LOYALTY] END RedemptionService.initiateRedemption | redemptionId={}", redemption.getId());
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
        log.info("[LOYALTY] START RedemptionService.verifyRedemption | redemptionId={}", id);
                Redemption redemption = getRedemptionWithLock(id);
        log.info("[LOYALTY] Redemption loaded | redemptionId={} | status={}", id, redemption.getStatus());

                redemption.assertStatus(RedemptionStatus.OTP_PENDING);
        log.info("[LOYALTY] OTP verification started | redemptionId={}", id);
                otpService.verifyOtp(redemption, otp);
        log.info("[LOYALTY] OTP verification succeeded | redemptionId={}", id);

                String authCode = otpService.generateAuthorizationCode();
        log.info("[LOYALTY] Authorization code generated | redemptionId={}", id);
                redemption.authorize(authCode, RESERVATION_TTL);
        log.info("[LOYALTY] Redemption authorized | redemptionId={} | reservationExpiresAt={}", id, redemption.getReservationExpiresAt());

        log.info("[LOYALTY] END RedemptionService.verifyRedemption | redemptionId={} | status={}", id, redemption.getStatus());
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
        log.info("[LOYALTY] START RedemptionService.commitRedemption | redemptionId={}", id);
                Redemption redemption = getRedemptionWithLock(id);
        log.info("[LOYALTY] Redemption loaded | redemptionId={} | status={}", id, redemption.getStatus());
                LoyaltyAccount account = redemption.getAccount();
                String idempKey = "RED_COMMIT_" + redemption.getId();
        log.info("[LOYALTY] Commit idempotency key generated | key={}", idempKey);

                if (redemption.getStatus() == RedemptionStatus.COMMITTED) {
            log.info("[LOYALTY] Idempotent commit retry detected | redemptionId={} | idempotencyKey={}", id, idempKey);
                        LoyaltyTransaction existingTx = loyaltyTransactionRepository.findByIdempotencyKey(idempKey)
                                .orElseThrow(() -> LoyaltyException.conflict(ErrorCode.INTERNAL_SERVER_ERROR,
                                        "Transaction not found for committed redemption"));
            log.info("[LOYALTY] Returning existing transaction | transactionId={}", existingTx.getId());
                        return responseMapper.mapToCommitResponse(redemption, existingTx, account);
                }

                redemption.assertStatus(RedemptionStatus.AUTHORIZED);

                if (!redemption.isAuthorizedWith(request.authorizationCode())) {
            log.warn("[LOYALTY] Commit validation failed - Invalid authorization code | redemptionId={}", id);
                        throw LoyaltyException.badRequest(ErrorCode.LOYALTY_OTP_INVALID, "Invalid authorization code");
                }
        log.info("[LOYALTY] Authorization validated | redemptionId={}", id);

                int points = redemption.getRequestedPoints();
        log.info("[LOYALTY] Finalizing reservation | redemptionId={} | points={}", id, points);
                account.finalizeReservation(points);
                redemption.commit();
        log.info("[LOYALTY] Redemption committed | redemptionId={}", id);

                LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                        .account(account)
                        .type(TransactionType.REDEEM)
                        .sourceTransactionId(redemption.getPurchaseTransactionId())
                        .points(points)
                        .moneyAmount(redemption.getDiscountAmount())
                        .status(TransactionStatus.COMMITTED)
                        .idempotencyKey(idempKey)
                        .build();
        log.info("[LOYALTY] Loyalty transaction created | redemptionId={} | points={}", id, points);

                loyaltyTransactionRepository.save(transaction);
        log.info("[LOYALTY] Loyalty transaction saved | transactionId={} | idempotencyKey={}", transaction.getId(), idempKey);

        log.info("[LOYALTY] END RedemptionService.commitRedemption | redemptionId={} | transactionId={}", id, transaction.getId());
                return responseMapper.mapToCommitResponse(redemption, transaction, account);
        }

        @Transactional
        public CancelResponseData cancelRedemption(Long id, CancelRequest request) {
                return cancelRedemptionInternal(id, request.reason());
        }

        @Transactional
        public CancelResponseData cancelRedemptionInternal(Long id, RedemptionCancelReason reason) {
        log.info("[LOYALTY] START RedemptionService.cancelRedemptionInternal | redemptionId={}", id);
                Redemption redemption = getRedemptionWithLock(id);
        log.info("[LOYALTY] Redemption loaded | redemptionId={} | status={}", id, redemption.getStatus());
                LoyaltyAccount account = redemption.getAccount();

                if (redemption.getStatus() == RedemptionStatus.COMMITTED) {
            log.warn("[LOYALTY] Cancel failed - already COMMITTED | redemptionId={}", id);
                        throw LoyaltyException.conflict(ErrorCode.LOYALTY_REDEMPTION_STATE_CONFLICT,
                                "Redemption is already COMMITTED, cannot cancel.");
                }

                if (redemption.getStatus() == RedemptionStatus.CANCELLED) {
            log.info("[LOYALTY] Redemption already cancelled | redemptionId={}", id);
                        return responseMapper.mapToCancelResponse(redemption, account);
                }

                redemption.getAllocations().forEach(allocation ->
                        allocation.getLot().restore(allocation.getPoints()));
        log.info("[LOYALTY] Allocations restored | redemptionId={}", id);

                int points = redemption.getRequestedPoints();
                account.releaseReservation(points);
        log.info("[LOYALTY] Reservation released | redemptionId={} | points={}", id, points);

                redemption.cancel(reason);
        log.info("[LOYALTY] Redemption cancelled | redemptionId={}", id);

        log.info("[LOYALTY] END RedemptionService.cancelRedemptionInternal | redemptionId={}", id);
                return responseMapper.mapToCancelResponse(redemption, account);
        }

        private Redemption getRedemptionWithLock(Long id) {
                return redemptionRepository.findByIdWithLock(id)
                        .orElseThrow(() -> LoyaltyException.notFound(ErrorCode.LOYALTY_ACCOUNT_NOT_FOUND,
                                "Redemption not found with id: " + id));
        }

        // helper method for freeze service
        private static final List<RedemptionStatus> ACTIVE_RESERVATION_STATUSES =
                List.of(RedemptionStatus.OTP_PENDING, RedemptionStatus.AUTHORIZED);

    @Transactional
    public int cancelActiveRedemptionsForAccount(Long accountId, RedemptionCancelReason reason) {
        List<Redemption> activeRedemptions =
                redemptionRepository.findByAccount_IdAndStatusIn(accountId, ACTIVE_RESERVATION_STATUSES);

        for (Redemption redemption : activeRedemptions) {
            cancelRedemptionInternal(redemption.getId(), reason);
        }

        return activeRedemptions.size();
    }

}