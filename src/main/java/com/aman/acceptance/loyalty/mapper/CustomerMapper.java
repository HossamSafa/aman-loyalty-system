package com.aman.acceptance.loyalty.mapper;

import com.aman.acceptance.loyalty.model.dto.response.BalanceDto;
import com.aman.acceptance.loyalty.model.dto.response.ConversionDto;
import com.aman.acceptance.loyalty.model.dto.response.ResolveCustomerResponse;
import com.aman.acceptance.loyalty.model.Customer;
import com.aman.acceptance.loyalty.model.LoyaltyAccount;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public ResolveCustomerResponse toResolveCustomerResponse (Customer customer, LoyaltyAccount account, Boolean newlyEnrolled ,String mobileNumberMasked)
    {
        return ResolveCustomerResponse.builder()
                .customerId(customer.getId())
                .accountId(account.getId())
                .programId(account.getProgram().getId())
                .newlyEnrolled(newlyEnrolled)
                .accountStatus(account.getStatus())
                .mobileNumberMasked(mobileNumberMasked)
                .balance(
                        BalanceDto.builder()
                                .available(account.getAvailablePoints())
                                .locked(account.getLockedPoints())
                                .reserved(account.getReservedPoints())
                                .totalOwned(account.getTotalOwned())
                                .build()
                )
                .conversion(
                        ConversionDto.builder()
                                .pointsPerEgp("1")
                                .egpPerPoint("0.005")
                                .build()
                )
                .build();
    }
}
