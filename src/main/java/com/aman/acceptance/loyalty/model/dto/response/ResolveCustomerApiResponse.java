package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveCustomerApiResponse {

    private boolean success;

    private ResolveCustomerResponse data;

    private MetaDto meta;

}