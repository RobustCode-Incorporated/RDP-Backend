package com.robustcode.delivery.user.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.dto.UserResponse;
import com.robustcode.delivery.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * ADMIN user management: list accounts (e.g. drivers pending approval) and
 * enable / disable them.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * Example: GET /api/admin/users?role=DRIVER&enabled=false lists pending drivers.
     */
    @GetMapping
    public List<UserResponse> findAll(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) User.Role role
    ) {
        return userService.findAll(enabled, role)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/enable")
    public UserResponse enable(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return UserResponse.from(
                userService.setEnabled(id, true, authentication.getName())
        );
    }

    @PatchMapping("/{id}/disable")
    public UserResponse disable(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return UserResponse.from(
                userService.setEnabled(id, false, authentication.getName())
        );
    }
}
