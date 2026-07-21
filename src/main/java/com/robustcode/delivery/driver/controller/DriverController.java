package com.robustcode.delivery.driver.controller;


import java.util.List;

import com.robustcode.delivery.driver.dto.CreateDriverRequest;
import com.robustcode.delivery.driver.dto.DriverResponse;
import com.robustcode.delivery.driver.service.DriverService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {


    private final DriverService driverService;


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<DriverResponse> findAll(){

        return driverService.findAll();

    }



    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody CreateDriverRequest request
    ){

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    driverService.createDriver(request)
                );

    }



    /**
     * DRIVER gets his own profile
     */
    @GetMapping("/me")
    public DriverResponse getMyDriver(
            Authentication authentication
    ){

        return driverService.findMyDriver(
                authentication.getName()
        );

    }

}