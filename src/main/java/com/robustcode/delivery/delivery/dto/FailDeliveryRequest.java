package com.robustcode.delivery.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailDeliveryRequest {

    @NotBlank(message = "Failure reason is required")
    @Size(max = 180, message = "Failure reason must be at most 180 characters")
    private String reason;
}