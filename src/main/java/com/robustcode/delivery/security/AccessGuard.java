package com.robustcode.delivery.security;

import org.springframework.stereotype.Component;

import com.robustcode.delivery.common.exception.ForbiddenException;
import com.robustcode.delivery.common.exception.NotFoundException;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ownership checks shared by services (object-level access control).
 * Role-level rules live in {@code SecurityConfig}; this class answers
 * "may this user see THIS restaurant / order / delivery".
 *
 * <p>Call it from inside a transaction so lazy associations
 * (restaurant, driver) can be resolved.</p>
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final UserRepository userRepository;

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public static boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }

    public static boolean isAdminOrDispatcher(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.DISPATCHER;
    }

    /**
     * Returns the id of the restaurant owned by a RESTAURANT user, or null.
     */
    public static Long ownRestaurantId(User user) {
        if (user.getRole() != User.Role.RESTAURANT || user.getRestaurant() == null) {
            return null;
        }
        return user.getRestaurant().getId();
    }

    /**
     * Returns the driver profile id of a DRIVER user, or null.
     */
    public static Long ownDriverId(User user) {
        if (user.getRole() != User.Role.DRIVER || user.getDriver() == null) {
            return null;
        }
        return user.getDriver().getId();
    }

    /**
     * ADMIN may access any restaurant; a RESTAURANT user only its own.
     */
    public void checkRestaurantAccess(User user, Long restaurantId) {
        if (isAdmin(user)) {
            return;
        }

        Long ownRestaurantId = ownRestaurantId(user);

        if (ownRestaurantId == null || !ownRestaurantId.equals(restaurantId)) {
            throw new ForbiddenException("You do not have access to this restaurant");
        }
    }
}
