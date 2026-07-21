package com.robustcode.delivery.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateDeliveryRequest {


    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotNull(message = "Restaurant id is required")
    private Long restaurantId;


    @NotBlank(message = "Pickup address is required")
    @Size(max = 255)
    private String pickupAddress;


    @NotBlank(message = "Delivery address is required")
    @Size(max = 255)
    private String deliveryAddress;


    @Size(max = 500)
    private String description;


}