package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.enums.AccountStatus;
import com.aman.acceptance.loyalty.enums.ErrorCode;
import com.aman.acceptance.loyalty.exception.LoyaltyException;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import org.springframework.stereotype.Service;

@Service
public class AccountStatusGuard {

    public void assertActive(LoyaltyAccount account) {
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw LoyaltyException.locked(
                    ErrorCode.LOYALTY_ACCOUNT_FROZEN,
                    "This loyalty account is temporarily frozen."
            );
        }
    }
}
