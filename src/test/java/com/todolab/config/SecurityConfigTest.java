package com.todolab.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("BCrypt PasswordEncoder를 제공한다")
    void passwordEncoder() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        String encoded = passwordEncoder.encode("password");

        assertThat(encoded).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", encoded)).isTrue();
    }
}
