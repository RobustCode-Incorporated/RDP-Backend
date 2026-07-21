package com.robustcode.delivery.driver.dto;


import com.robustcode.delivery.driver.domain.Driver;



public record DriverResponse(


        Long id,


        Long userId,


        String firstName,


        String lastName,


        String email,


        String phoneNumber,


        String vehicleType,


        String vehiclePlate,


        Driver.AvailabilityStatus availabilityStatus


) {}