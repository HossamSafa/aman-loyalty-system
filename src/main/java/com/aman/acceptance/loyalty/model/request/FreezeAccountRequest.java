package com.aman.acceptance.loyalty.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;

@Getter
@Setter
public class FreezeAccountRequest {

    @NotBlank(message = "reasonCode is required")
    private String reasonCode;

    private String note;

    @NotBlank(message = "actorId is required")
    private String actorId;
}
