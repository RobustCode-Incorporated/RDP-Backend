package com.robustcode.delivery.user.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates or resets the platform ADMIN account at startup.
 *
 * <p>Enabled only when both environment variables are set:</p>
 * <ul>
 *   <li>{@code ADMIN_BOOTSTRAP_EMAIL} - admin login email</li>
 *   <li>{@code ADMIN_BOOTSTRAP_PASSWORD} - admin password (min 12 characters)</li>
 * </ul>
 *
 * <p>If the account exists, its password is reset (bcrypt), its role set to
 * ADMIN and it is enabled; otherwise it is created. Runs after Flyway, on
 * every startup while the variables are set: remove them from the
 * environment once the admin has logged in. The password is never logged.</p>
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final String email;

    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
            @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (email.isEmpty() && password.isEmpty()) {
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            log.warn("AdminBootstrap skipped: both ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD must be set");
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters"
            );
        }

        User admin = userRepository.findByEmail(email).orElse(null);
        boolean created = admin == null;

        if (created) {
            admin = User.builder()
                    .email(email)
                    .firstName("Platform")
                    .lastName("Admin")
                    .build();
        }

        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setRestaurant(null);
        admin.setEnabled(true);

        userRepository.save(admin);

        log.info("AdminBootstrap: {} ADMIN account {} (password set from ADMIN_BOOTSTRAP_PASSWORD)",
                created ? "created" : "reset", email);
    }
}
