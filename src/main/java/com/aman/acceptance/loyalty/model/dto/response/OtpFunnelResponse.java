package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpFunnelResponse {
    private Long reserved;
    private Long verified;
    private Long committed;
}