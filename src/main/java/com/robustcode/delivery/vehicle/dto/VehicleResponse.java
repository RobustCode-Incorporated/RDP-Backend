package com.robustcode.delivery.vehicle.dto;


import com.robustcode.delivery.vehicle.domain.Vehicle;



public record VehicleResponse(


        Long id,


        String vehicleType,


        String plateNumber,


        Vehicle.Status status


) {}