package com.robustcode.delivery.user.service;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.dto.CreateUserRequest;
import com.robustcode.delivery.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService {


    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }



    public User createUser(CreateUserRequest request){

        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }


        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .enabled(true)
                .build();


        return userRepository.save(user);
    }

}