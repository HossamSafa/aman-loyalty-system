package com.aman.acceptance.loyalty.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ResolveCustomerRequest {
    @NotBlank(message = "phone number is required")
    @Pattern(
        regexp = "^(\\+20|0)?1[0125][0-9]{8}$",
        message = "Invalid number"
    )
    private String mobileNumber;

    private String customerName;

    private Boolean autoEnroll;

}
