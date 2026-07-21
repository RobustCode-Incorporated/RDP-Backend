package com.robustcode.delivery.auth.controller;

import com.robustcode.delivery.auth.dto.LoginRequest;
import com.robustcode.delivery.auth.dto.LoginResponse;
import com.robustcode.delivery.auth.dto.RegisterRequest;
import com.robustcode.delivery.auth.dto.RegisterRestaurantRequest;
import com.robustcode.delivery.auth.service.AuthService;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @PostMapping("/register-restaurant")
    public ResponseEntity<UserResponse> registerRestaurant(
            @Valid @RequestBody RegisterRestaurantRequest request
    ) {
        User user = authService.registerRestaurant(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}