package com.aman.acceptance.loyalty.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnfreezeAccountRequest {

    @NotBlank(message = "reasonCode is required")
    private String reasonCode;

    private String note;

    @NotBlank(message = "actorId is required")
    private String actorId;
}
