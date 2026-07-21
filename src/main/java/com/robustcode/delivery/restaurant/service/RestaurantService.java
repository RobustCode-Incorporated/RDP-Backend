package com.robustcode.delivery.restaurant.service;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.dto.CreateRestaurantRequest;
import com.robustcode.delivery.restaurant.dto.RestaurantResponse;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantService {


    private final RestaurantRepository repository;

    private final UserRepository userRepository;



    public RestaurantResponse createRestaurant(
            CreateRestaurantRequest request
    ){


        if(request.email() != null 
                && repository.existsByEmail(request.email())){

            throw new RuntimeException(
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




    public RestaurantResponse findById(Long id){

        Restaurant restaurant = repository.findById(id)

                .orElseThrow(() ->
                        new RuntimeException(
                                "Restaurant not found"
                        )
                );


        return RestaurantResponse.from(restaurant);

    }


    @Transactional(readOnly = true)
    public RestaurantResponse findMyRestaurant(String email){

        User user = userRepository.findByEmail(email)

                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        if(user.getRestaurant() == null){
            throw new RuntimeException(
                    "No restaurant assigned to user"
            );
        }

        return RestaurantResponse.from(
                user.getRestaurant()
        );

    }

}