package com.example.ProjectX.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.ProjectX.dto.UserRegistrationRequestDto;
import com.example.ProjectX.dto.UserResponseDto;
import com.example.ProjectX.exception.EmailFoundException;
import com.example.ProjectX.exception.PasswordException;
import com.example.ProjectX.model.Role;
import com.example.ProjectX.model.User;
import com.example.ProjectX.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto register(UserRegistrationRequestDto user) {
        if (!user.password().equals(user.repeatPassword())) {
            throw new PasswordException("Password do not match");
        }

        if (userRepository.findByEmail(user.email()).isPresent()) {
            throw new EmailFoundException("A user with this email address exists");
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

    private UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(user.getId(), user.getEmail());
    }
}
