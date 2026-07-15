package com.robustcode.delivery.order.service;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.driver.domain.Driver;
import com.robustcode.delivery.driver.repository.DriverRepository;
import com.robustcode.delivery.order.domain.Order;
import com.robustcode.delivery.order.domain.OrderStatusHistory;
import com.robustcode.delivery.order.dto.CreateOrderRequest;
import com.robustcode.delivery.order.dto.OrderResponse;
import com.robustcode.delivery.order.dto.OrderStatusHistoryResponse;
import com.robustcode.delivery.order.repository.OrderRepository;
import com.robustcode.delivery.order.repository.OrderStatusHistoryRepository;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {


    private final OrderRepository orderRepository;

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    private final RestaurantRepository restaurantRepository;

    private final UserRepository userRepository;

    private final DriverRepository driverRepository;



    private boolean isValidTransition(
            Order.Status current,
            Order.Status next
    ){

        return switch(current){

            case PENDING ->
                    next == Order.Status.ACCEPTED;

            case ACCEPTED ->
                    next == Order.Status.PREPARING;

            case PREPARING ->
                    next == Order.Status.READY_FOR_PICKUP;

            case READY_FOR_PICKUP ->
                    next == Order.Status.PICKED_UP;

            case PICKED_UP ->
                    next == Order.Status.DELIVERED;

            default ->
                    false;
        };
    }




    public OrderResponse createOrder(
            CreateOrderRequest request,
            String customerEmail
    ){

        User customer =
                userRepository.findByEmail(customerEmail)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found")
                );


        Restaurant restaurant =
                restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() ->
                        new RuntimeException("Restaurant not found")
                );


        Order order = Order.builder()

                .customer(customer)

                .restaurant(restaurant)

                .status(Order.Status.PENDING)

                .totalPrice(request.totalPrice())

                .deliveryAddress(request.deliveryAddress())

                .build();


        Order savedOrder =
                orderRepository.save(order);



        saveHistory(
                savedOrder,
                null,
                Order.Status.PENDING,
                customerEmail
        );


        return OrderResponse.from(savedOrder);

    }




    public OrderResponse updateStatus(
            Long orderId,
            Order.Status newStatus,
            String email
    ){

        Order order =
                orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found")
                );


        Order.Status oldStatus =
                order.getStatus();



        if(!isValidTransition(oldStatus,newStatus)){

            throw new RuntimeException(
                    "Invalid order status transition"
            );

        }



        order.setStatus(newStatus);



        Order savedOrder =
                orderRepository.save(order);



        saveHistory(
                savedOrder,
                oldStatus,
                newStatus,
                email
        );



        return OrderResponse.from(savedOrder);

    }




    public OrderResponse acceptOrder(
            Long orderId,
            String email
    ){

        verifyRestaurantOwner(orderId,email);


        return updateStatus(
                orderId,
                Order.Status.ACCEPTED,
                email
        );

    }




    public OrderResponse startPreparing(
            Long orderId,
            String email
    ){

        verifyRestaurantOwner(orderId,email);


        return updateStatus(
                orderId,
                Order.Status.PREPARING,
                email
        );

    }




    public OrderResponse readyForPickup(
            Long orderId,
            String email
    ){

        verifyRestaurantOwner(orderId,email);


        return updateStatus(
                orderId,
                Order.Status.READY_FOR_PICKUP,
                email
        );

    }




    public OrderResponse pickupOrder(
            Long orderId,
            String email
    ){

        verifyAssignedDriver(orderId,email);


        return updateStatus(
                orderId,
                Order.Status.PICKED_UP,
                email
        );

    }





    public OrderResponse deliverOrder(
            Long orderId,
            String email
    ){

        verifyAssignedDriver(orderId,email);


        OrderResponse response =
                updateStatus(
                        orderId,
                        Order.Status.DELIVERED,
                        email
                );


        Order order =
                orderRepository.findById(orderId)
                .orElseThrow();



        Driver driver =
                order.getDriver();


        if(driver != null){

            driver.setAvailabilityStatus(
                    Driver.AvailabilityStatus.AVAILABLE
            );

            driverRepository.save(driver);

        }



        return response;

    }





    public OrderResponse assignDriver(
            Long orderId,
            Long driverId
    ){

        Order order =
                orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found")
                );



        Driver driver =
                driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new RuntimeException("Driver not found")
                );



        if(driver.getAvailabilityStatus()
                != Driver.AvailabilityStatus.AVAILABLE){

            throw new RuntimeException(
                    "Driver is not available"
            );

        }



        order.setDriver(driver);



        driver.setAvailabilityStatus(
                Driver.AvailabilityStatus.BUSY
        );


        driverRepository.save(driver);



        Order savedOrder =
                orderRepository.save(order);



        return OrderResponse.from(savedOrder);

    }





    private void verifyRestaurantOwner(
            Long orderId,
            String email
    ){

        Order order =
                orderRepository.findById(orderId)
                .orElseThrow();



        User user =
                userRepository.findByEmail(email)
                .orElseThrow();



        if(user.getRestaurant()==null ||
                !order.getRestaurant()
                .getId()
                .equals(user.getRestaurant().getId())
        ){

            throw new RuntimeException(
                    "Unauthorized restaurant access"
            );

        }

    }





    private void verifyAssignedDriver(
            Long orderId,
            String email
    ){

        Order order =
                orderRepository.findById(orderId)
                .orElseThrow();



        if(order.getDriver()==null ||
                !order.getDriver()
                .getUser()
                .getEmail()
                .equals(email)
        ){

            throw new RuntimeException(
                    "Driver not assigned to this order"
            );

        }

    }





    private void saveHistory(
            Order order,
            Order.Status oldStatus,
            Order.Status newStatus,
            String changedBy
    ){

        OrderStatusHistory history =
                OrderStatusHistory.builder()

                .order(order)

                .oldStatus(oldStatus)

                .newStatus(newStatus)

                .changedBy(changedBy)

                .build();



        orderStatusHistoryRepository.save(history);

    }





    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(
            String email
    ){

        User customer =
                userRepository.findByEmail(email)
                .orElseThrow();


        return orderRepository.findByCustomer(customer)

                .stream()

                .map(OrderResponse::from)

                .toList();

    }





    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(
            String email
    ){

        User user =
                userRepository.findByEmail(email)
                .orElseThrow();



        Restaurant restaurant =
                user.getRestaurant();



        if(restaurant==null){

            throw new RuntimeException(
                    "Restaurant not assigned"
            );

        }



        return orderRepository.findByRestaurant(restaurant)

                .stream()

                .map(OrderResponse::from)

                .toList();

    }





    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(){

        return orderRepository.findAll()

                .stream()

                .map(OrderResponse::from)

                .toList();

    }



        @Transactional(readOnly = true)
        public List<OrderStatusHistoryResponse> getOrderStatusHistory(
                        Long orderId
        ){

                return orderStatusHistoryRepository.findByOrderId(orderId)

                                .stream()

                                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))

                                .map(OrderStatusHistoryResponse::from)

                                .toList();

        }

}