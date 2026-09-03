package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.RedemptionCancelReason;
import com.aman.acceptance.loyalty.enums.RedemptionStatus;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.repository.RedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedemptionScheduler {

    private final RedemptionRepository redemptionRepository;
    private final RedemptionService redemptionService;

    @Scheduled(fixedDelay = 60000) // run every minute
    public void cancelExpiredRedemptions() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Expired OTP validations
        List<Redemption> expiredOtpPending = redemptionRepository.findByStatusAndOtpExpiresAtBefore(RedemptionStatus.OTP_PENDING, now);
        for (Redemption redemption : expiredOtpPending) {
            try {
                redemptionService.cancelRedemptionInternal(redemption.getId(), RedemptionCancelReason.OTP_EXPIRED);
                log.info("Automatically cancelled OTP_PENDING redemption id {}", redemption.getId());
            } catch (Exception e) {
                log.error("Failed to cleanly cancel OTP_PENDING redemption id {}", redemption.getId(), e);
            }
        }

        // Expired AUTHORIZED reservations
        List<Redemption> expiredAuthorized = redemptionRepository.findByStatusAndReservationExpiresAtBefore(RedemptionStatus.AUTHORIZED, now);
        for (Redemption redemption : expiredAuthorized) {
            try {
                redemptionService.cancelRedemptionInternal(redemption.getId(), RedemptionCancelReason.POS_TIMEOUT);
                log.info("Automatically cancelled AUTHORIZED redemption id {}", redemption.getId());
            } catch (Exception e) {
                log.error("Failed to cleanly cancel AUTHORIZED redemption id {}", redemption.getId(), e);
            }
        }
    }
}
