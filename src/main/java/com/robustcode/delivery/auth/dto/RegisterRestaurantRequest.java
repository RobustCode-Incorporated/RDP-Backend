package com.robustcode.delivery.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record RegisterRestaurantRequest(


        @NotBlank(message = "Restaurant name is required")
        String restaurantName,


        String description,


        @NotBlank(message = "Phone number is required")
        String phoneNumber,


        @NotBlank(message = "Restaurant email is required")
        @Email(message = "Email must be valid")
        String restaurantEmail,


        @NotBlank(message = "Address is required")
        String address,


        String city,


        String country,



        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be valid")
        String adminEmail,



        @NotBlank(message = "Admin password is required")
        String adminPassword,



        @NotBlank(message = "Admin first name is required")
        String adminFirstName,



        @NotBlank(message = "Admin last name is required")
        String adminLastName


) {

}