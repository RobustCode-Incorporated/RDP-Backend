package com.robustcode.delivery.user.controller;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.user.dto.CreateUserRequest;
import com.robustcode.delivery.user.dto.UserResponse;
import com.robustcode.delivery.user.service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;


    public UserController(UserService userService){
        this.userService = userService;
    }


    /**
     * ADMIN only: create a user with any role.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ){

        return UserResponse.from(
                userService.createUser(request)
        );
    }

}
