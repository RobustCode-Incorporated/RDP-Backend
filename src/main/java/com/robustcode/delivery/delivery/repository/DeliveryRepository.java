package com.robustcode.delivery.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.driver.domain.Driver;

/**
 * Deliveries are serialized directly by the controllers (open-in-view is
 * off), so restaurant and driver are fetched eagerly here to avoid
 * LazyInitializationException (500) during JSON rendering.
 */
public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    @Override
    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findAll();

    @Override
    @EntityGraph(attributePaths = {"restaurant", "driver"})
    Optional<Delivery> findById(Long id);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByRestaurant(Restaurant restaurant);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByRestaurantId(Long restaurantId);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByDriver(Driver driver);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByDriverId(Long driverId);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByStatus(Delivery.Status status);

    @EntityGraph(attributePaths = {"restaurant", "driver"})
    List<Delivery> findByRestaurantIdAndStatus(
            Long restaurantId,
            Delivery.Status status
    );
}
