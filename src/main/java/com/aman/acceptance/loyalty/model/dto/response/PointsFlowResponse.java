package com.aman.acceptance.loyalty.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointsFlowResponse {
    private String month;
    private Long issued;
    private Long redeemed;
}