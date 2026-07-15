package com.robustcode.delivery.order.dto;

import java.time.LocalDateTime;

import com.robustcode.delivery.order.domain.Order;
import com.robustcode.delivery.order.domain.OrderStatusHistory;

public record OrderStatusHistoryResponse(

        Long id,

        Order.Status oldStatus,

        Order.Status newStatus,

        String changedBy,

        LocalDateTime createdAt

) {

    public static OrderStatusHistoryResponse from(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedBy(),
                history.getCreatedAt()
        );
    }
}