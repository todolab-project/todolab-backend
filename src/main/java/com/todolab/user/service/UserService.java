package com.todolab.user.service;

import com.todolab.auth.dto.RegisterRequest;
import com.todolab.user.domain.User;
import com.todolab.user.dto.UserResponse;
import com.todolab.user.exception.UserEmailAlreadyExistsException;
import com.todolab.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserEmailAlreadyExistsException(email);
        }

        User saved = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName()
        ));

        return UserResponse.from(saved);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
