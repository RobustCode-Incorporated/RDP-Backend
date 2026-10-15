package com.robustcode.delivery.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDriverRequest {

    @NotNull(message = "Driver id is required")
    private Long driverId;

}