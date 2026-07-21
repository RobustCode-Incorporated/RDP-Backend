package com.robustcode.delivery.user.controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.user.dto.CreateUserRequest;
import com.robustcode.delivery.user.dto.UserResponse;
import com.robustcode.delivery.user.service.UserService;


@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;


    public UserController(UserService userService){
        this.userService = userService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @RequestBody CreateUserRequest request
    ){

        return UserResponse.from(
                userService.createUser(request)
        );
    }

}