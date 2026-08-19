package com.aman.acceptance.loyalty.model.request;

import com.aman.acceptance.loyalty.enums.AdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdjustmentRequest {

    @NotNull(message = "type is required")
    private AdjustmentType type;

    @NotNull(message = "points is required")
    @Positive(message = "points must be greater than zero")
    private Integer points;

    @NotBlank(message = "reasonCode is required")
    private String reasonCode;

    private String note;

    private Integer expiresInDays;

    @NotBlank(message = "actorId is required")
    private String actorId;
}
