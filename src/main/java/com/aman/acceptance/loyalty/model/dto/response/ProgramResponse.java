package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.enums.ProgramStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponse {

    private Long id;
    private String merchantId;
    private String name;
    private ProgramStatus status;
    private String currency;
    private Integer lockDays;
    private Integer expiryDays;
}