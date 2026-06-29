package com.todolab.user.service;

import com.todolab.auth.dto.RegisterRequest;
import com.todolab.user.domain.User;
import com.todolab.user.dto.UserResponse;
import com.todolab.user.exception.UserEmailAlreadyExistsException;
import com.todolab.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 시 이메일을 정규화하고 비밀번호를 해시로 저장한다")
    void register_success() {
        RegisterRequest request = new RegisterRequest(" TEST@Example.COM ", "password123", "테스터");
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.displayName()).isEqualTo("테스터");

        then(userRepository).should().existsByEmail("test@example.com");
        then(passwordEncoder).should().encode("password123");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest(" TEST@Example.COM ", "password123", "테스터");
        given(userRepository.existsByEmail("test@example.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        then(userRepository).should().existsByEmail("test@example.com");
        then(passwordEncoder).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoMoreInteractions();
    }
}
