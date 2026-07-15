package com.robustcode.delivery.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.robustcode.delivery.order.domain.Order;

public record OrderResponse(

        Long id,

        Long restaurantId,

        Long customerId,

        Long driverId,

        Order.Status status,

        BigDecimal totalAmount,

        LocalDateTime createdAt

) {

    public static OrderResponse from(Order order) {

        return new OrderResponse(

                order.getId(),

                order.getRestaurant() != null
                        ? order.getRestaurant().getId()
                        : null,

                order.getCustomer() != null
                        ? order.getCustomer().getId()
                        : null,

                order.getDriver() != null
                        ? order.getDriver().getId()
                        : null,

                order.getStatus(),

                order.getTotalPrice(),

                order.getCreatedAt()

        );

    }

}