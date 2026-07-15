package com.robustcode.delivery.vehicle.controller;


import com.robustcode.delivery.vehicle.dto.CreateVehicleRequest;
import com.robustcode.delivery.vehicle.dto.VehicleResponse;
import com.robustcode.delivery.vehicle.service.VehicleService;


import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;



@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {



    private final VehicleService vehicleService;




    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request
    ){

        return ResponseEntity.ok(
                vehicleService.createVehicle(request)
        );

    }





    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles(){

        return ResponseEntity.ok(
                vehicleService.getAllVehicles()
        );

    }


}