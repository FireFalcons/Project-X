package com.example.ProjectX.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.ProjectX.dto.login.UserLoginRequestDto;
import com.example.ProjectX.dto.login.UserLoginResponseDto;
import com.example.ProjectX.dto.register.UserRegistrationRequestDto;
import com.example.ProjectX.dto.register.UserRegistrationResponseDto;
import com.example.ProjectX.exception.login.EmailFoundException;
import com.example.ProjectX.exception.login.PasswordException;
import com.example.ProjectX.exception.register.AuthException;
import com.example.ProjectX.model.Role;
import com.example.ProjectX.model.User;
import com.example.ProjectX.repository.UserRepository;
import com.example.ProjectX.token.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserRegistrationResponseDto register(UserRegistrationRequestDto user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            throw new EmailFoundException("A user with this email address exists");
        }

        if (user.password() == null) {
            throw new PasswordException("Password field is not specified");
        }

        String hashedPassword = passwordEncoder.encode(user.password());
        User savedUser = userRepository.save(toEntity(user, hashedPassword));
        return toResponseDto(savedUser);
    }

    private User toEntity(UserRegistrationRequestDto requestDto, String hashedPassword) {
        User user = new User();
        user.setEmail(requestDto.email());
        user.setPassword(hashedPassword);
        user.setRole(Role.USER);
        return user;
    }

    private UserRegistrationResponseDto toResponseDto(User user) {
        return new UserRegistrationResponseDto(user.getId(), user.getEmail());
    }

    public UserLoginResponseDto login(UserLoginRequestDto user) {
        User currentUser = userRepository.findByEmail(user.email())
                .orElseThrow(() -> new AuthException("Invalid email or password"));
        if(!passwordEncoder.matches(user.password(), currentUser.getPassword())) {
            throw new AuthException("Invalid email or password");
        }
        return new UserLoginResponseDto(jwtTokenProvider.generateToken(currentUser.getEmail(), currentUser.getRole().name()));
    }
}
