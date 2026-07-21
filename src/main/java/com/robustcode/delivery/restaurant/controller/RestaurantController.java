package com.robustcode.delivery.restaurant.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import com.robustcode.delivery.restaurant.dto.CreateRestaurantRequest;
import com.robustcode.delivery.restaurant.dto.RestaurantResponse;
import com.robustcode.delivery.restaurant.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {


    private final RestaurantService service;



    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse create(
            @Valid @RequestBody CreateRestaurantRequest request
    ){

        return service.createRestaurant(request);

    }



    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<RestaurantResponse> findAll(){

        return service.findAll();

    }


    @PreAuthorize("hasRole('RESTAURANT')")
    @GetMapping("/me")
    public RestaurantResponse getMyRestaurant(
            Authentication authentication
    ){

        return service.findMyRestaurant(
                authentication.getName()
        );

    }


    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT')")
    @GetMapping("/{id}")
    public RestaurantResponse findById(
            @PathVariable Long id
    ){

        return service.findById(id);

    }

}