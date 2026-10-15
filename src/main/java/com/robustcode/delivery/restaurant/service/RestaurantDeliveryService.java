package com.robustcode.delivery.restaurant.service;

import com.robustcode.delivery.common.exception.NotFoundException;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.security.AccessGuard;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantDeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccessGuard accessGuard;

    /**
     * ADMIN sees any restaurant's deliveries; RESTAURANT only its own.
     */
    public List<Delivery> getRestaurantDeliveries(Long restaurantId, String email) {

        accessGuard.checkRestaurantAccess(accessGuard.requireUser(email), restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        return deliveryRepository.findByRestaurant(restaurant);
    }

    public List<Delivery> getRestaurantDeliveriesByStatus(
            Long restaurantId,
            Delivery.Status status,
            String email) {

        accessGuard.checkRestaurantAccess(accessGuard.requireUser(email), restaurantId);

        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        return deliveryRepository.findByRestaurantIdAndStatus(
                restaurantId,
                status);
    }
}
