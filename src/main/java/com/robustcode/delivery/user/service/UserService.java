package com.robustcode.delivery.user.service;

import java.util.List;

import com.robustcode.delivery.common.exception.BusinessRuleException;
import com.robustcode.delivery.common.exception.ConflictException;
import com.robustcode.delivery.common.exception.NotFoundException;
import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.dto.CreateUserRequest;
import com.robustcode.delivery.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
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



    /**
     * ADMIN creates a user with any role. The account is enabled immediately.
     */
    public User createUser(CreateUserRequest request){

        if(userRepository.existsByEmail(request.email())){
            throw new ConflictException(
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



    @Transactional(readOnly = true)
    public List<User> findAll(Boolean enabled, User.Role role){

        return userRepository.findAll()
                .stream()
                .filter(user -> enabled == null || user.isEnabled() == enabled)
                .filter(user -> role == null || user.getRole() == role)
                .toList();
    }



    /**
     * ADMIN approves (enables) or suspends (disables) an account.
     * An admin cannot disable their own account.
     */
    public User setEnabled(Long userId, boolean enabled, String actingAdminEmail){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!enabled && user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new BusinessRuleException("You cannot disable your own account");
        }

        user.setEnabled(enabled);

        return userRepository.save(user);
    }

}
