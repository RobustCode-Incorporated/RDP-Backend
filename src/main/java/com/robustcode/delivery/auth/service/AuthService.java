package com.robustcode.delivery.auth.service;

import com.robustcode.delivery.auth.dto.LoginRequest;
import com.robustcode.delivery.auth.dto.LoginResponse;
import com.robustcode.delivery.auth.dto.RegisterRequest;
import com.robustcode.delivery.auth.dto.RegisterRestaurantRequest;
import com.robustcode.delivery.restaurant.domain.Restaurant;
import com.robustcode.delivery.restaurant.repository.RestaurantRepository;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;
import com.robustcode.delivery.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {


    private final UserRepository userRepository;

    private final RestaurantRepository restaurantRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;




    public User register(RegisterRequest request) {


        if (userRepository.existsByEmail(request.email())) {

            throw new RuntimeException(
                    "Email already exists"
            );

        }



        User user = User.builder()

                .email(request.email())

                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )

                .firstName(request.firstName())

                .lastName(request.lastName())

                .role(request.role())

                .enabled(true)

                .build();



        return userRepository.save(user);

    }

    public User registerRestaurant(
            RegisterRestaurantRequest request
    ) {

        if(userRepository.existsByEmail(request.adminEmail())) {

            throw new RuntimeException(
                    "Admin email already exists"
            );

        }

        if (restaurantRepository.existsByEmail(request.restaurantEmail())) {

            throw new RuntimeException(
                    "Restaurant email already exists"
            );

        }

        Restaurant restaurant = Restaurant.builder()

                .name(request.restaurantName())

                .description(request.description())

                .phoneNumber(request.phoneNumber())

                .email(request.restaurantEmail())

                .address(request.address())

                .city(request.city())

                .country(request.country())

                .status(Restaurant.Status.ACTIVE)

                .build();


        Restaurant savedRestaurant =
                restaurantRepository.save(restaurant);


        User user = User.builder()

                .email(request.adminEmail())

                .password(
                        passwordEncoder.encode(
                                request.adminPassword()
                        )
                )

                .firstName(request.adminFirstName())

                .lastName(request.adminLastName())

                .role(User.Role.RESTAURANT)

                .restaurant(savedRestaurant)

                .enabled(true)

                .build();


        return userRepository.save(user);

    }





    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {



        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        request.email(),

                        request.password()

                )

        );



        User user = userRepository.findByEmail(request.email())

                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        String token = jwtService.generateToken(user);


        return new LoginResponse(

                token,

                user.getEmail(),

                user.getRole()

        );

    }


}