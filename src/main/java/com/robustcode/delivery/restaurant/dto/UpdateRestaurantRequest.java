package com.robustcode.delivery.restaurant.dto;

import java.math.BigDecimal;

import com.robustcode.delivery.restaurant.domain.Restaurant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial update ({@code PATCH /api/restaurants/{id}}): null fields are left
 * unchanged. latitude/longitude must be sent together.
 */
public record UpdateRestaurantRequest(

        @Size(min = 1, max = 150, message = "Restaurant name must be 1-150 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 30, message = "Phone number must be at most 30 characters")
        String phoneNumber,

        @Email(message = "Email should be valid")
        String email,

        @Size(min = 1, max = 255, message = "Address must be 1-255 characters")
        String address,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        BigDecimal longitude,

        Restaurant.Status status

) {
}
