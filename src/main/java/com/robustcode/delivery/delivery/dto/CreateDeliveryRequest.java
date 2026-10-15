package com.robustcode.delivery.delivery.dto;

import java.math.BigDecimal;

import com.robustcode.delivery.delivery.domain.Delivery;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/deliveries}.
 *
 * <p>{@code restaurantId} is required for ADMIN/DISPATCHER and ignored for a
 * RESTAURANT user (their own restaurant is always used). Cross-field rules
 * (cash on delivery, coordinate pairs) are checked in the service.</p>
 */
public record CreateDeliveryRequest(

        Long restaurantId,

        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Customer name must be at most 150 characters")
        String customerName,

        @NotBlank(message = "Customer phone is required")
        @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE)
        String customerPhone,

        @NotBlank(message = "Delivery address is required")
        @Size(max = 255, message = "Delivery address must be at most 255 characters")
        String deliveryAddress,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        BigDecimal deliveryLatitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        BigDecimal deliveryLongitude,

        @Size(max = 255, message = "Pickup address must be at most 255 characters")
        String pickupAddress,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        BigDecimal pickupLatitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        BigDecimal pickupLongitude,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes,

        @Size(max = 100, message = "Order reference must be at most 100 characters")
        String orderReference,

        @DecimalMin(value = "0.00", message = "Order amount must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "Order amount must have at most 2 decimals")
        BigDecimal orderAmount,

        @NotNull(message = "Payment mode is required (PREPAID or CASH_ON_DELIVERY)")
        Delivery.PaymentMode paymentMode,

        @Digits(integer = 8, fraction = 2, message = "Cash to collect must have at most 2 decimals")
        BigDecimal cashToCollect,

        @DecimalMin(value = "0.00", message = "Delivery fee must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "Delivery fee must have at most 2 decimals")
        BigDecimal deliveryFee

) {
}
