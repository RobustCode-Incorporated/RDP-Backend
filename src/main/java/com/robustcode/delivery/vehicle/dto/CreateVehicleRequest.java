package com.robustcode.delivery.vehicle.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;



public record CreateVehicleRequest(


        @NotBlank(message = "Vehicle type is required")
        @Size(
                max = 50,
                message = "Vehicle type must not exceed 50 characters"
        )
        String vehicleType,



        @NotBlank(message = "Plate number is required")
        @Size(
                max = 50,
                message = "Plate number must not exceed 50 characters"
        )
        String plateNumber


) {}