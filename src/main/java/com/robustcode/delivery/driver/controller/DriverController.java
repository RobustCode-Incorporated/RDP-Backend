package com.robustcode.delivery.driver.controller;


import java.util.List;

import com.robustcode.delivery.driver.dto.CreateDriverRequest;
import com.robustcode.delivery.driver.dto.DriverResponse;
import com.robustcode.delivery.driver.dto.UpdateDriverLocationRequest;
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


    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @GetMapping
    public List<DriverResponse> findAll(){

        return driverService.findAll();

    }



    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me")
    public DriverResponse getMyDriver(
            Authentication authentication
    ){

        return driverService.findMyDriver(
                authentication.getName()
        );

    }



    @PreAuthorize("hasRole('DRIVER')")
    @PatchMapping("/me/location")
    public DriverResponse updateMyLocation(
            Authentication authentication,
            @Valid @RequestBody UpdateDriverLocationRequest request
    ) {

        return driverService.updateMyLocation(
                authentication.getName(),
                request
        );

    }

}