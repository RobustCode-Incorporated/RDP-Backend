package com.robustcode.delivery.delivery.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class DeliveryResponse {


    private Long id;

    private String pickupAddress;

    private String deliveryAddress;

    private String description;

    private String status;

    private BigDecimal price;

}