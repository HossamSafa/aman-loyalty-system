package com.aman.acceptance.loyalty.model.dto.response;

import com.aman.acceptance.loyalty.model.dto.common.MetaDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {

    private boolean success;
    private CustomerDto data;
    @Builder.Default
    private MetaDto meta = MetaDto.now();

    public CustomerResponseDto(boolean success, CustomerDto data) {
        this.success = success;
        this.data = data;
        this.meta = MetaDto.now();
    }
}