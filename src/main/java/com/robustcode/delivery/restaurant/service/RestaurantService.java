package com.robustcode.delivery.restaurant.service;

import com.robustcode.delivery.common.exception.BusinessRuleException;
import com.robustcode.delivery.common.exception.ConflictException;
import com.robustcode.delivery.common.exception.NotFoundException;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.dto.CreateRestaurantRequest;
import com.robustcode.delivery.restaurant.dto.RestaurantResponse;
import com.robustcode.delivery.restaurant.dto.UpdateRestaurantRequest;
import com.robustcode.delivery.common.exception.FieldValidationException;
import java.math.RoundingMode;
import java.util.Map;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.security.AccessGuard;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantService {


    private final RestaurantRepository repository;

    private final UserRepository userRepository;

    private final AccessGuard accessGuard;



    public RestaurantResponse createRestaurant(
            CreateRestaurantRequest request
    ){


        if(request.email() != null 
                && repository.existsByEmail(request.email())){

            throw new ConflictException(
                    "Restaurant email already exists"
            );

        }


        Restaurant restaurant = Restaurant.builder()

                .name(request.name())

                .description(request.description())

                .phoneNumber(request.phoneNumber())

                .email(request.email())

                .address(request.address())

                .city(request.city())

                .country(request.country())

                .status(Restaurant.Status.ACTIVE)

                .build();



        return RestaurantResponse.from(
                repository.save(restaurant)
        );

    }




    public List<RestaurantResponse> findAll(){

        return repository.findAll()
                .stream()
                .map(RestaurantResponse::from)
                .toList();

    }




    /**
     * ADMIN sees any restaurant; RESTAURANT only its own.
     */
    @Transactional(readOnly = true)
    public RestaurantResponse findById(Long id, String email){

        accessGuard.checkRestaurantAccess(accessGuard.requireUser(email), id);

        Restaurant restaurant = repository.findById(id)

                .orElseThrow(() ->
                        new NotFoundException(
                                "Restaurant not found"
                        )
                );


        return RestaurantResponse.from(restaurant);

    }


    @Transactional(readOnly = true)
    public RestaurantResponse findMyRestaurant(String email){

        User user = userRepository.findByEmail(email)

                .orElseThrow(() ->
                        new NotFoundException(
                                "User not found"
                        )
                );

        if(user.getRestaurant() == null){
            throw new BusinessRuleException(
                    "No restaurant assigned to user"
            );
        }

        return RestaurantResponse.from(
                user.getRestaurant()
        );

    }



    /**
     * ADMIN only (SecurityConfig). Partial update; coordinates are the
     * default pickup point of the restaurant's new deliveries.
     */
    public RestaurantResponse update(Long id, UpdateRestaurantRequest request){

        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        if((request.latitude() == null) != (request.longitude() == null)){
            throw new FieldValidationException(Map.of(
                    "latitude", "latitude and longitude must be provided together"));
        }

        if(request.email() != null
                && !request.email().equalsIgnoreCase(restaurant.getEmail())
                && repository.existsByEmail(request.email())){
            throw new ConflictException("Restaurant email already exists");
        }

        if(request.name() != null) restaurant.setName(request.name().trim());
        if(request.description() != null) restaurant.setDescription(request.description());
        if(request.phoneNumber() != null) restaurant.setPhoneNumber(request.phoneNumber().trim());
        if(request.email() != null) restaurant.setEmail(request.email().trim());
        if(request.address() != null) restaurant.setAddress(request.address().trim());
        if(request.city() != null) restaurant.setCity(request.city());
        if(request.country() != null) restaurant.setCountry(request.country());
        if(request.status() != null) restaurant.setStatus(request.status());
        if(request.latitude() != null){
            restaurant.setLatitude(request.latitude().setScale(7, RoundingMode.HALF_UP));
            restaurant.setLongitude(request.longitude().setScale(7, RoundingMode.HALF_UP));
        }

        return RestaurantResponse.from(repository.saveAndFlush(restaurant));

    }

}
