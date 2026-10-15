

package com.robustcode.delivery.restaurant.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.restaurant.service.RestaurantDeliveryService;

import lombok.RequiredArgsConstructor;

/**
 * ADMIN, or RESTAURANT for its own restaurant only.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/deliveries")
@RequiredArgsConstructor
public class RestaurantDeliveryController {

    private final RestaurantDeliveryService restaurantDeliveryService;

    @GetMapping
    public List<Delivery> getRestaurantDeliveries(
            @PathVariable Long restaurantId,
            Authentication authentication) {

        return restaurantDeliveryService.getRestaurantDeliveries(
                restaurantId,
                authentication.getName());
    }

    @GetMapping("/status")
    public List<Delivery> getRestaurantDeliveriesByStatus(
            @PathVariable Long restaurantId,
            @RequestParam Delivery.Status status,
            Authentication authentication) {

        return restaurantDeliveryService.getRestaurantDeliveriesByStatus(
                restaurantId,
                status,
                authentication.getName());
    }
}