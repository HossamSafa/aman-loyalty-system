package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.LotStatus;
import com.aman.acceptance.loyalty.exception.AccountException;
import com.aman.acceptance.loyalty.exception.ResourceNotFoundException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.PointsLot;
import com.aman.acceptance.loyalty.model.responses.*;
import com.aman.acceptance.loyalty.repository.LoyaltyAccountRepository;
import com.aman.acceptance.loyalty.repository.LoyaltyTransactionRepository;
import com.aman.acceptance.loyalty.repository.PointsLotRepository;
import com.aman.acceptance.loyalty.utilies.validators.LoyaltyAccounHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import com.aman.acceptance.loyalty.model.dto.response.LoyaltyAccountResponseDto;
import com.aman.acceptance.loyalty.util.MobileUtil;
import com.aman.acceptance.loyalty.util.PhoneMaskingUtil;

@Service
@RequiredArgsConstructor
public class LoyaltyAccountService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PointsLotRepository pointsLotRepository;
    private final LoyaltyAccounHelper loyaltyAccounHelper;
    private final MobileUtil mobileUtil;

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

    public Page<LoyaltyAccountResponseDto> getEnrolledAccounts(Pageable pageable) {
        Page<LoyaltyAccount> accounts = loyaltyAccountRepository.findAll(pageable);
        return accounts.map(this::toLoyaltyAccountResponseDto);
    }

    private LoyaltyAccountResponseDto toLoyaltyAccountResponseDto(LoyaltyAccount account) {
        return LoyaltyAccountResponseDto.builder()
                .accountId(account.getId())
                .customerMobile(PhoneMaskingUtil.maskPhoneNumber(mobileUtil.decryptMobile(account.getCustomer().getMobileEncrypted())))
                .programName(account.getProgram().getName())
                .availablePoints(account.getAvailablePoints())
                .lockedPoints(account.getLockedPoints())
                .reservedPoints(account.getReservedPoints())
                .status(account.getStatus().name())
                .enrolledDate(account.getCreatedAt().toLocalDate())
                .build();
    }


}

