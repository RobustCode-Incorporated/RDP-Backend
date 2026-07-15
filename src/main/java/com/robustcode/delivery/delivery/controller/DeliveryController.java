package com.robustcode.delivery.delivery.controller;


import java.util.List;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import com.robustcode.delivery.delivery.domain.Delivery;
import com.robustcode.delivery.delivery.dto.AssignDriverRequest;
import com.robustcode.delivery.delivery.dto.CreateDeliveryRequest;
import com.robustcode.delivery.delivery.dto.FailDeliveryRequest;
import com.robustcode.delivery.delivery.service.DeliveryService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {


    private final DeliveryService service;


    @GetMapping
    public List<Delivery> getAll(){

        return service.findAll();

    }


    @GetMapping("/me")
    public List<Delivery> getMyDeliveries(
            Authentication authentication
    ){

        return service.findMyDeliveries(authentication.getName());

    }


    @GetMapping("/{id}")
    public Delivery getById(
            @PathVariable Long id){

        return service.findById(id);

    }


    @PostMapping
    public Delivery create(
            @Valid @RequestBody CreateDeliveryRequest request){

        return service.create(request);

    }


    @PutMapping("/{id}/assign")
    public Delivery assignDriver(
            @PathVariable Long id,
            @RequestBody AssignDriverRequest request){

        return service.assignDriver(
                id,
                request.getDriverId()
        );

    }


    @PutMapping("/{id}/pickup")
    public Delivery pickupDelivery(
            @PathVariable Long id,
            Authentication authentication){

        return service.pickupDelivery(id, authentication.getName());

    }


    @PutMapping("/{id}/start")
    public Delivery startDelivery(
            @PathVariable Long id,
            Authentication authentication){

        return service.startDelivery(id, authentication.getName());

    }


    @PutMapping("/{id}/complete")
    public Delivery completeDelivery(
            @PathVariable Long id,
            Authentication authentication){

        return service.completeDelivery(id, authentication.getName());

    }


    @PutMapping("/{id}/fail")
    public Delivery failDelivery(
            @PathVariable Long id,
            @Valid @RequestBody FailDeliveryRequest request,
            Authentication authentication
    ){

        return service.failDelivery(
                id,
                authentication.getName(),
                request.getReason()
        );

    }

}