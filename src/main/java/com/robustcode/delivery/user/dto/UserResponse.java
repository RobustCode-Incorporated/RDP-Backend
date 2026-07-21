package com.robustcode.delivery.user.dto;


import com.robustcode.delivery.user.domain.User;


public record UserResponse(

        Long id,

        String email,

        String firstName,

        String lastName,

        User.Role role

) {


    public static UserResponse from(User user){

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}