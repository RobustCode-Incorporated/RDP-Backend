

package com.robustcode.delivery.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.repository.DeliveryRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantDeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;

    public List<Delivery> getRestaurantDeliveries(Long restaurantId) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return deliveryRepository.findByRestaurant(restaurant);
    }

    public List<Delivery> getRestaurantDeliveriesByStatus(
            Long restaurantId,
            Delivery.Status status) {

        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return deliveryRepository.findByRestaurantIdAndStatus(
                restaurantId,
                status);
    }
}