package com.robustcode.delivery.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.restaurant.domain.Restaurant;

/**
 * API representation of a delivery. Timestamps are ISO-8601 UTC instants.
 */
public record DeliveryResponse(

        Long id,
        Delivery.Status status,
        Long version,

        RestaurantSummary restaurant,
        DriverSummary driver,

        String customerName,
        String customerPhone,

        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,

        String deliveryAddress,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,

        String description,
        String notes,
        String orderReference,
        BigDecimal orderAmount,
        Delivery.PaymentMode paymentMode,
        BigDecimal cashToCollect,
        BigDecimal deliveryFee,

        String failureReason,
        String cancellationReason,

        Instant createdAt,
        Instant updatedAt,
        Instant readyAt,
        Instant assignedAt,
        Instant pickedUpAt,
        Instant deliveredAt,
        Instant failedAt,
        Instant cancelledAt

) {

    public record RestaurantSummary(
            Long id,
            String name,
            String address,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        static RestaurantSummary from(Restaurant restaurant) {
            return new RestaurantSummary(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhoneNumber(),
                    restaurant.getLatitude(),
                    restaurant.getLongitude());
        }
    }

    public record DriverSummary(
            Long id,
            String firstName,
            String lastName,
            String phone,
            String vehicleType,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant lastLocationAt
    ) {
        static DriverSummary from(Driver driver) {
            return new DriverSummary(
                    driver.getId(),
                    driver.getUser() != null ? driver.getUser().getFirstName() : null,
                    driver.getUser() != null ? driver.getUser().getLastName() : null,
                    driver.getPhoneNumber(),
                    driver.getVehicleType(),
                    driver.getCurrentLatitude(),
                    driver.getCurrentLongitude(),
                    toInstant(driver.getLastLocationAt()));
        }
    }

    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.getId(),
                d.getStatus(),
                d.getVersion(),
                RestaurantSummary.from(d.getRestaurant()),
                d.getDriver() != null ? DriverSummary.from(d.getDriver()) : null,
                d.getCustomerName(),
                d.getCustomerPhone(),
                d.getPickupAddress(),
                d.getPickupLatitude(),
                d.getPickupLongitude(),
                d.getDeliveryAddress(),
                d.getDeliveryLatitude(),
                d.getDeliveryLongitude(),
                d.getDescription(),
                d.getNotes(),
                d.getOrderReference(),
                d.getOrderAmount(),
                d.getPaymentMode(),
                d.getCashToCollect(),
                d.getDeliveryFee(),
                d.getFailureReason(),
                d.getCancellationReason(),
                toInstant(d.getCreatedAt()),
                toInstant(d.getUpdatedAt()),
                toInstant(d.getReadyAt()),
                toInstant(d.getAssignedAt()),
                toInstant(d.getPickedUpAt()),
                toInstant(d.getDeliveredAt()),
                toInstant(d.getFailedAt()),
                toInstant(d.getCancelledAt()));
    }

    /** Entities store server-local LocalDateTime; the API exposes UTC instants. */
    public static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
