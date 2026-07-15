package com.robustcode.delivery.restaurant.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robustcode.delivery.restaurant.domain.Restaurant;


public interface RestaurantRepository 
        extends JpaRepository<Restaurant, Long>{


    Optional<Restaurant> findByEmail(String email);


    boolean existsByEmail(String email);

}