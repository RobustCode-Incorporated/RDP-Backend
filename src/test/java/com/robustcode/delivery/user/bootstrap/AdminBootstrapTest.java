package com.robustcode.delivery.user.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.robustcode.delivery.user.domain.User;
import com.robustcode.delivery.user.repository.UserRepository;

@SpringBootTest
class AdminBootstrapTest {

    private static final String EMAIL = "bootstrap-admin@rdp.test";

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void createsThenResetsAdmin() {
        new AdminBootstrap(userRepository, passwordEncoder, EMAIL, "first-strong-password").run(null);

        User created = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(created.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(created.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches("first-strong-password", created.getPassword())).isTrue();

        created.setEnabled(false);
        userRepository.save(created);

        new AdminBootstrap(userRepository, passwordEncoder, EMAIL, "second-strong-password").run(null);

        User reset = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(reset.getId()).isEqualTo(created.getId());
        assertThat(reset.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches("second-strong-password", reset.getPassword())).isTrue();
    }

    @Test
    void doesNothingWhenNotConfigured() {
        new AdminBootstrap(userRepository, passwordEncoder, "", "").run(null);
        new AdminBootstrap(userRepository, passwordEncoder, EMAIL, "").run(null);

        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    void rejectsWeakPassword() {
        assertThatThrownBy(() -> new AdminBootstrap(userRepository, passwordEncoder, EMAIL, "short").run(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
