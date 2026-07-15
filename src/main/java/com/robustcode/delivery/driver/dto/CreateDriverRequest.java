package com.robustcode.delivery.driver.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



public record CreateDriverRequest(


        @NotNull(message = "User id is required")
        Long userId,


        @NotBlank(message = "Phone number is required")
        @Size(
            min = 8,
            max = 20,
            message = "Phone number must contain between 8 and 20 characters"
        )
        String phoneNumber,


        @NotBlank(message = "Vehicle type is required")
        @Size(
            max = 50,
            message = "Vehicle type too long"
        )
        String vehicleType,


        @NotBlank(message = "Vehicle plate is required")
        @Size(
            max = 30,
            message = "Vehicle plate too long"
        )
        String vehiclePlate


) {}