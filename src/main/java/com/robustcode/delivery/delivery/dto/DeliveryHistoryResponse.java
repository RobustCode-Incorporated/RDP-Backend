package com.robustcode.delivery.delivery.dto;

import java.time.Instant;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.domain.DeliveryStatusHistory;

public record DeliveryHistoryResponse(
        Long id,
        DeliveryStatusHistory.Event event,
        Delivery.Status oldStatus,
        Delivery.Status newStatus,
        String changedBy,
        String note,
        Instant createdAt
) {

    public static DeliveryHistoryResponse from(DeliveryStatusHistory h) {
        return new DeliveryHistoryResponse(
                h.getId(),
                h.getEvent(),
                h.getOldStatus(),
                h.getNewStatus(),
                h.getChangedBy(),
                h.getNote(),
                DeliveryResponse.toInstant(h.getCreatedAt()));
    }
}
