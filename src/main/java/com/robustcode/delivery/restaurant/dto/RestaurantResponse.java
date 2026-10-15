package com.robustcode.delivery.restaurant.dto;

import java.math.BigDecimal;


import com.robustcode.delivery.restaurant.domain.Restaurant;


public record RestaurantResponse(


        Long id,

        String name,

        String description,

        String phoneNumber,

        String email,

        String address,

        String city,

        String country,

        BigDecimal latitude,

        BigDecimal longitude,

        Restaurant.Status status


) {


    public static RestaurantResponse from(Restaurant restaurant){


        return new RestaurantResponse(

                restaurant.getId(),

                restaurant.getName(),

                restaurant.getDescription(),

                restaurant.getPhoneNumber(),

                restaurant.getEmail(),

                restaurant.getAddress(),

                restaurant.getCity(),

                restaurant.getCountry(),

                restaurant.getLatitude(),

                restaurant.getLongitude(),

                restaurant.getStatus()

        );

    }

}