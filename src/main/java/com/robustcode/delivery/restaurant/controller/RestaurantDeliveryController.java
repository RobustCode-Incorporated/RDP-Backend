package com.robustcode.delivery.restaurant.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.dto.DeliveryResponse;
import com.robustcode.delivery.delivery.service.DeliveryService;

import lombok.RequiredArgsConstructor;

/**
 * Kept for v1 compatibility; same data as {@code GET /api/deliveries} for a
 * restaurant. ADMIN, or RESTAURANT for its own restaurant only.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/deliveries")
@RequiredArgsConstructor
public class RestaurantDeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @GetMapping
    public List<DeliveryResponse> getRestaurantDeliveries(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) List<Delivery.Status> status,
            Authentication authentication) {

        return deliveryService.searchForRestaurant(restaurantId, status, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @GetMapping("/status")
    public List<DeliveryResponse> getRestaurantDeliveriesByStatus(
            @PathVariable Long restaurantId,
            @RequestParam Delivery.Status status,
            Authentication authentication) {

        return deliveryService.searchForRestaurant(restaurantId, List.of(status), authentication.getName());
    }
}
