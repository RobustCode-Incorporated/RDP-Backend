

package com.robustcode.delivery.auth.dto;

import com.robustcode.delivery.user.domain.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String phoneNumber,

        String vehicleType,

        String vehiclePlate,

        /**
         * Only DRIVER or CUSTOMER are accepted; any other role is rejected with 400.
         */
        @NotNull(message = "Role is required")
        User.Role role

) {
}