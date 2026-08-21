package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.enums.TransactionStatus;
import com.aman.acceptance.loyalty.enums.TransactionType;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.responses.ExpireBatchResult;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aman.acceptance.loyalty.exception.AccountException;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPointsLifecycleService {

    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    public List<PointsLot>

    findLockedLotsForUnlock(final Pageable pageable,final LotStatus status,

                            final LocalDateTime unlockAt, final Integer remainingPoints) {

        log.info("Finding locked lots for unlock: status={}, unlockAt={}, remainingPoints={}, pageSize={}",

                status, unlockAt, remainingPoints, pageable.getPageSize());
      final List<PointsLot> lockedLots =  pointsLotRepository

                .findByStatusAndUnlockAtLessThanEqualAndRemainingPointsGreaterThan(
                        pageable,status,unlockAt,remainingPoints);

        log.info("Found {} locked lots for unlock", lockedLots.size());

      return lockedLots;

    }

    public void transferStatusOfPointsToAvailable(final PointsLot pointsLot) {

        pointsLot.setStatus(LotStatus.AVAILABLE);

    }

    public void movePointsFromLockedToAvailable
            (final LoyaltyAccount loyaltyAccount, final PointsLot pointsLot) throws AccountException {

        if (loyaltyAccount.getLockedPoints() < pointsLot.getRemainingPoints()) {

            throw new AccountException("Insufficient locked points");

        }

       final int lockedPoints = loyaltyAccount.getLockedPoints()-pointsLot.getRemainingPoints();

       final int availablePoints = loyaltyAccount.getAvailablePoints()+pointsLot.getRemainingPoints();

       loyaltyAccount.setLockedPoints(lockedPoints);

       loyaltyAccount.setAvailablePoints(availablePoints);

    }

    @Transactional
        public int processUnlocks(final Pageable pageable, final LotStatus status,
        final LocalDateTime unlockAt, final Integer remainingPoints) throws AccountException {

        log.info("Starting points unlock process: status={}, unlockAt={}, remainingPoints={}, batchSize={}",

                status, unlockAt, remainingPoints, pageable.getPageSize());

            final List<PointsLot> lockedLots = findLockedLotsForUnlock(pageable, status, unlockAt, remainingPoints);

        log.info("Found {} locked lots to unlock", lockedLots.size());

            for (final PointsLot pointsLot : lockedLots) {

                final LoyaltyAccount loyaltyAccount =

                        loyaltyAccountRepository.findByIdForUpdate(pointsLot.getAccount().getId()).get();

                movePointsFromLockedToAvailable(loyaltyAccount, pointsLot);

                transferStatusOfPointsToAvailable(pointsLot);

            }

            return lockedLots.size();

    }

    public List<PointsLot> findExpiredLots(

            final Pageable pageable, final LotStatus status, final LocalDateTime expiresAt,

            final Integer remainingPoints, final Long checkpointId) {

        log.info("========== EXPIRE DEBUG ==========");
        log.info("Finding expired points lots");
        log.info("status = {}", status);
        log.info("expiresAt = {}", expiresAt);
        log.info("remainingPoints = {}", remainingPoints);
        log.info("checkpointId = {}", checkpointId);
        log.info("pageSize = {}", pageable.getPageSize());
        log.info("pageNumber = {}", pageable.getPageNumber());

        final List<PointsLot> lots = pointsLotRepository

                        .findByStatusAndExpiresAtLessThanEqualAndRemainingPointsGreaterThanAndIdGreaterThanOrderByIdAsc(

                                status, expiresAt, remainingPoints, checkpointId, pageable);

        System.out.println("FOUND LOTS = " + lots.size());

        return lots;
    }

    public void expirePointsLot(final PointsLot pointsLot) {

        pointsLot.setStatus(LotStatus.EXPIRED);

    }

    public void movePointsFromAvailableToExpired
            (final LoyaltyAccount loyaltyAccount, final PointsLot pointsLot) throws AccountException {

        if (loyaltyAccount.getAvailablePoints() < pointsLot.getRemainingPoints()) {

            throw new AccountException("Insufficient available points");

        }

        final int expiredPoints = pointsLot.getRemainingPoints();

        final int newAvailablePoints = loyaltyAccount.getAvailablePoints()-expiredPoints;

        loyaltyAccount.setAvailablePoints(newAvailablePoints);

        pointsLot.setRemainingPoints(0);

    }


    @Transactional
    public ExpireBatchResult processExpire

            (final Pageable pageable,final LotStatus status, final LocalDateTime expiresAt,

             final Integer remainingPoints,final Long checkpointId) throws AccountException {

        log.info("========== EXPIRE PROCESS START ==========");
        log.info("Starting expire batch processing");
        log.info("status = {}", status);
        log.info("expiresAt = {}", expiresAt);
        log.info("remainingPoints = {}", remainingPoints);
        log.info("checkpointId = {}", checkpointId);
        log.info("pageSize = {}", pageable.getPageSize());
        log.info("pageNumber = {}", pageable.getPageNumber());

        final List<PointsLot> expiredLots = findExpiredLots(pageable,status,expiresAt,remainingPoints,checkpointId);

        log.info("Expired lots found = {}", expiredLots.size());

         int expiredPoints = 0;

        for (final PointsLot pointsLot : expiredLots) {

            log.info("---------- Processing PointsLot ----------");
            log.info("PointsLot id = {}", pointsLot.getId());
            log.info("Account id = {}", pointsLot.getAccount().getId());
            log.info("Remaining points = {}", pointsLot.getRemainingPoints());
            log.info("Lot status = {}", pointsLot.getStatus());
            log.info("Expires at = {}", pointsLot.getExpiresAt());

            final int pointsToExpire = pointsLot.getRemainingPoints();

            final LoyaltyAccount loyaltyAccount =

                    loyaltyAccountRepository.findByIdForUpdate(pointsLot.getAccount().getId()).get();

            log.info("Locked account for update. Account id = {}", loyaltyAccount.getId());

            log.info("Account points before expiration: available={}, locked={}, reserved={}",
                    loyaltyAccount.getAvailablePoints(),
                    loyaltyAccount.getLockedPoints(),
                    loyaltyAccount.getReservedPoints());

            movePointsFromAvailableToExpired(loyaltyAccount, pointsLot);

            log.info("Moved {} points from available to expired for lot {}", pointsToExpire, pointsLot.getId());

            expirePointsLot(pointsLot);
            log.info("PointsLot {} status changed to {}", pointsLot.getId(), pointsLot.getStatus());

            createExpireTransaction(loyaltyAccount, pointsLot, pointsToExpire);

            log.info("Expire transaction created for lot {}", pointsLot.getId());

            expiredPoints += pointsToExpire;

            log.info("Current expired points total = {}", expiredPoints);
        }

        if (expiredLots.isEmpty()) {

            log.info("No expired lots found. Returning null.");
            log.info("========== EXPIRE PROCESS END ==========");

            return null;

        }

        final int processedLots = expiredLots.size();

        final PointsLot lastProcessedLot = expiredLots.get(expiredLots.size() - 1);

        final Long lastProcessedLotId = lastProcessedLot.getId().longValue();

        final String checkPoint = "lot-".concat(lastProcessedLotId.toString());

        log.info("========== EXPIRE BATCH RESULT ==========");
        log.info("processedLots = {}", processedLots);
        log.info("expiredPoints = {}", expiredPoints);
        log.info("lastProcessedLotId = {}", lastProcessedLotId);
        log.info("checkpoint = {}", checkPoint);
        log.info("========== EXPIRE PROCESS END ==========");

        return new ExpireBatchResult( "EXPIRE_POINTS",processedLots,expiredPoints,checkPoint);

    }

    public void createExpireTransaction(final LoyaltyAccount loyaltyAccount,

                                         final PointsLot pointsLot, final int expiredPoints) {

        final String sourceTransactionId = "LOT-" + pointsLot.getId();

        final boolean alreadyExists = loyaltyTransactionRepository

                .findBySourceTransactionIdAndType(sourceTransactionId, TransactionType.EXPIRE).isPresent();

        if (alreadyExists) {

            return;

        }

        final LoyaltyTransaction transaction = LoyaltyTransaction.builder()

                .account(loyaltyAccount)

                .type(TransactionType.EXPIRE)

                .sourceTransactionId(sourceTransactionId)

                .points(expiredPoints)

                .status(TransactionStatus.COMMITTED)

                .build();

        loyaltyTransactionRepository.save(transaction);

    }


}
