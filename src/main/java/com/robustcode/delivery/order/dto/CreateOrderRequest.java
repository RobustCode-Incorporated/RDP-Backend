package com.robustcode.delivery.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

        @NotNull
        Long restaurantId,

        @NotNull
        BigDecimal totalPrice,

        @NotBlank
        String deliveryAddress

) {
}