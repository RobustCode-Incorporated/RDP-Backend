package com.robustcode.delivery.delivery.dto;

import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/deliveries/{id}/cancel}. The body itself is optional.
 */
public record CancelDeliveryRequest(

        @Size(max = 500, message = "Cancellation reason must be at most 500 characters")
        String reason

) {
}
