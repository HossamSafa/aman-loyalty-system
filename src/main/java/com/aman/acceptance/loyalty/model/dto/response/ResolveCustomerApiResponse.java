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
    @Builder.Default
    private MetaDto meta = MetaDto.now();

    public ResolveCustomerApiResponse(boolean success, ResolveCustomerResponse data) {
        this.success = success;
        this.data = data;
        this.meta = MetaDto.now();
    }
}