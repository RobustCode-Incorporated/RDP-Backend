package com.robustcode.delivery.delivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.driver.domain.Driver;

public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    List<Delivery> findByRestaurant(Restaurant restaurant);

    List<Delivery> findByRestaurantId(Long restaurantId);

    List<Delivery> findByDriver(Driver driver);

    List<Delivery> findByDriverId(Long driverId);

    List<Delivery> findByStatus(Delivery.Status status);

    List<Delivery> findByRestaurantIdAndStatus(
            Long restaurantId,
            Delivery.Status status
    );
}