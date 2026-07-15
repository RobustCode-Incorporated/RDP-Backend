package com.robustcode.delivery.restaurant.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record CreateRestaurantRequest(


        @NotBlank(message = "Restaurant name is required")
        String name,


        String description,


        String phoneNumber,


        @Email(message = "Email should be valid")
        String email,


        @NotBlank(message = "Address is required")
        String address,


        String city,


        String country


) {}