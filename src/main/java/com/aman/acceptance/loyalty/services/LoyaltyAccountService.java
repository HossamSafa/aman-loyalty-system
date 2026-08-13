package com.aman.acceptance.loyalty.services;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.exceptions.AccountException;
import com.aman.acceptance.loyalty.exceptions.ResourceNotFoundException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.responses.*;
import com.aman.acceptance.loyalty.repositries.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repositries.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repositries.PointsLotRepository;
import com.aman.acceptance.loyalty.utilies.validators.LoyaltyAccounHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class LoyaltyAccountService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyAccounHelper loyaltyAccounHelper;

    /**findAccount*/
    public AccountResponse findAccount(final Long accountId) {

       final LoyaltyAccount loyaltyAccount = findAccountById(accountId);

       final BalanceResponse balanceResponse = viewBalanceResponse(loyaltyAccount) ;

       final NearestExpiryResponse nearestExpiryResponse = getNearestExpiryResponseGreaterThanZero(accountId);

       final ConversionResponse conversionResponse = loyaltyAccounHelper.buildConversion();

       return new AccountResponse(String.valueOf(loyaltyAccount.getCustomer().getId()),
               String.valueOf(loyaltyAccount.getId()),
               String.valueOf(loyaltyAccount.getProgram().getId()),loyaltyAccount.getStatus().name(),
               LoyaltyAccounHelper.mobileNumberMasked(loyaltyAccount.getCustomer().getMobileEncrypted()),
               balanceResponse, nearestExpiryResponse,conversionResponse);
    }

    /**getTransactions*/
    public TransactionPageResponse getTransactions(final Long accountId, final Pageable pageable) {
        loyaltyAccounHelper.validateAccountExists(accountId);
        Page<LoyaltyTransaction> transactionPage = loyaltyTransactionRepository.findByAccount_Id(accountId,pageable);
        return toTransactionPageResponse(transactionPage);
    }

    /**findAccountById*/
    public LoyaltyAccount findAccountById(final Long accountId) throws AccountException {
        return loyaltyAccountRepository.findById(accountId)
                .orElseThrow(()-> new AccountException("this is Account Is Not Found"));
    }

    /**convert LoyaltyTransaction to TransactionPageResponse*/
    public TransactionPageResponse toTransactionPageResponse(final Page<LoyaltyTransaction> transactionPage) {
        return new TransactionPageResponse(transactionPage.map(this::toTransactionResponse).getContent(),
                transactionPage.getNumber(), transactionPage.getSize(),
                transactionPage.getTotalElements(),transactionPage.getTotalPages());}

    /**convert LoyaltyTransaction to TransactionResponse*/
    public TransactionResponse toTransactionResponse(final LoyaltyTransaction transaction) {

        return new TransactionResponse(String.valueOf(transaction.getId()), transaction.getType().name(),

                transaction.getPoints().longValue(), transaction.getStatus().name(),
                transaction.getSourceTransactionId(),
                transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**convert PointsLot to NearestExpiryResponse*/
    public NearestExpiryResponse toNearestExpiryResponse(final PointsLot pointsLot) {
      return new NearestExpiryResponse(
        pointsLot.getRemainingPoints().longValue(),
              pointsLot.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**convert NearestExpiryResponse to getNearestExpiryResponseGreaterThanZero*/
    public NearestExpiryResponse
    getNearestExpiryResponseGreaterThanZero(final Long accountId) throws ResourceNotFoundException {
        return pointsLotRepository
                .findFirstByAccount_IdAndStatusAndRemainingPointsGreaterThanOrderByExpiresAtAscUnlockAtAsc
                        (accountId, LotStatus.AVAILABLE,0)
                .map(this::toNearestExpiryResponse).orElseThrow(() ->
                        new ResourceNotFoundException("No available points found for account: " + accountId));
    }

    /**viewBalanceResponse*/
    public BalanceResponse viewBalanceResponse(final LoyaltyAccount loyaltyAccount) {
        return new BalanceResponse(loyaltyAccount.getAvailablePoints()
                ,loyaltyAccount.getLockedPoints(),
                loyaltyAccount.getReservedPoints(),loyaltyAccount.getTotalOwned());
    }


}

