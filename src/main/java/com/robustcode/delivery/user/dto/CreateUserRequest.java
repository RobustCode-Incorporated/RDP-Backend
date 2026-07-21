package com.robustcode.delivery.user.dto;

import com.robustcode.delivery.user.domain.User;

public record CreateUserRequest(

        String email,

        String password,

        String firstName,

        String lastName,

        User.Role role

) {
}