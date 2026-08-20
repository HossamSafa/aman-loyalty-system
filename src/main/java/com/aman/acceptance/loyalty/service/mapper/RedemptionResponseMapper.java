package com.aman.acceptance.loyalty.service.mapper;

import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.LoyaltyTransaction;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.model.dto.response.BalanceDto;
import com.aman.acceptance.loyalty.model.dto.response.CancelResponseData;
import com.aman.acceptance.loyalty.model.dto.response.CommitResponseData;
import com.aman.acceptance.loyalty.model.dto.response.RedemptionMoneyDto;
import org.springframework.stereotype.Component;

@Component
public class RedemptionResponseMapper {

    public CommitResponseData mapToCommitResponse(Redemption redemption, LoyaltyTransaction transaction, LoyaltyAccount account) {
        return new CommitResponseData(
                "red-" + redemption.getId(),
                redemption.getStatus(),
                (long) redemption.getRequestedPoints(),
                new RedemptionMoneyDto(redemption.getDiscountAmount(), "EGP"),
                "ltx-" + transaction.getId(),
                createBalanceDto(account)
        );
    }

    public CancelResponseData mapToCancelResponse(Redemption redemption, LoyaltyAccount account) {
        return new CancelResponseData(
                "red-" + redemption.getId(),
                redemption.getStatus(),
                (long) redemption.getRequestedPoints(),
                createBalanceDto(account)
        );
    }

    private BalanceDto createBalanceDto(LoyaltyAccount account) {
        return new BalanceDto(
                account.getAvailablePoints(),
                account.getLockedPoints(),
                account.getReservedPoints(),
                account.getTotalOwned()
        );
    }
}
