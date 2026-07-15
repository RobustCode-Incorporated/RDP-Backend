package com.robustcode.delivery.order.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.robustcode.delivery.order.dto.CreateOrderRequest;
import com.robustcode.delivery.order.dto.OrderResponse;
import com.robustcode.delivery.order.dto.OrderStatusHistoryResponse;
import com.robustcode.delivery.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {


    private final OrderService orderService;


    /**
     * CUSTOMER creates an order
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ){

        return orderService.createOrder(
                request,
                authentication.getName()
        );

    }


    /**
     * CUSTOMER sees his own orders
     */
    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(
            Authentication authentication
    ){

        return orderService.getMyOrders(
                authentication.getName()
        );

    }


    /**
     * RESTAURANT sees orders assigned to his restaurant
     */
    @GetMapping("/restaurant")
    public List<OrderResponse> getRestaurantOrders(
            Authentication authentication
    ){

        return orderService.getRestaurantOrders(
                authentication.getName()
        );

    }


    /**
     * ADMIN sees all orders
     */
    @GetMapping
    public List<OrderResponse> findAll(){

        return orderService.findAll();

    }


        /**
         * RESTAURANT, DRIVER and ADMIN inspect the order status history
         */
        @GetMapping("/{id}/history")
        public List<OrderStatusHistoryResponse> getOrderHistory(
                        @PathVariable Long id
        ){

                return orderService.getOrderStatusHistory(id);

        }


    /**
     * RESTAURANT accepts order
     */
    @PatchMapping("/{id}/accept")
    public OrderResponse acceptOrder(
            @PathVariable Long id,
            Authentication authentication
    ){

        return orderService.acceptOrder(
                id,
                authentication.getName()
        );

    }


    /**
     * RESTAURANT starts preparing order
     */
    @PatchMapping("/{id}/prepare")
    public OrderResponse prepareOrder(
            @PathVariable Long id,
            Authentication authentication
    ){

        return orderService.startPreparing(
                id,
                authentication.getName()
        );

    }


    /**
     * RESTAURANT marks order ready for pickup
     */
    @PatchMapping("/{id}/ready")
    public OrderResponse readyOrder(
            @PathVariable Long id,
            Authentication authentication
    ){

        return orderService.readyForPickup(
                id,
                authentication.getName()
        );

    }


    /**
     * DRIVER picks up order
     */
    @PatchMapping("/{id}/pickup")
    public OrderResponse pickupOrder(
            @PathVariable Long id,
            Authentication authentication
    ){

        return orderService.pickupOrder(
                id,
                authentication.getName()
        );

    }


    /**
     * DRIVER delivers order
     */
    @PatchMapping("/{id}/deliver")
    public OrderResponse deliverOrder(
            @PathVariable Long id,
            Authentication authentication
    ){

        return orderService.deliverOrder(
                id,
                authentication.getName()
        );

    }


    /**
     * ADMIN assigns driver to order
     */
    @PatchMapping("/{id}/assign-driver/{driverId}")
    public OrderResponse assignDriver(
            @PathVariable Long id,
            @PathVariable Long driverId,
            Authentication authentication
    ){

        return orderService.assignDriver(
                id,
                driverId
        );

    }

}