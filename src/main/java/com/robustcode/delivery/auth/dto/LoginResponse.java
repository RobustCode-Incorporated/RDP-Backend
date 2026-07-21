package com.robustcode.delivery.auth.dto;

import com.robustcode.delivery.user.domain.User;

public record LoginResponse(

        String token,

        String email,

        User.Role role

) {
}