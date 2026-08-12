package com.aman.acceptance.loyalty.service;

import com.aman.acceptance.loyalty.model.dto.response.OtpMetadataDto;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import com.aman.acceptance.loyalty.model.Redemption;
import com.aman.acceptance.loyalty.util.PhoneMaskingUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OtpService {

    public OtpMetadataDto initiate(LoyaltyAccount account, Redemption redemption) {
        String phoneNumber = account.getCustomer().getMobileEncrypted();
        String maskedPhone = PhoneMaskingUtil.maskPhoneNumber(phoneNumber);
        Instant expiresAt = Instant.now().plusSeconds(120);

        // TODO: Dispatch actual OTP code (e.g. via SMS provider).
        // IMPORTANT: If this involves an external call, it must NOT happen inside the active transaction.
        // Consider dispatching a Spring application event here using @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        // so the SMS is sent only after the Redemption commit succeeds.
        
        return new OtpMetadataDto(maskedPhone, expiresAt, 3);
    }
}
