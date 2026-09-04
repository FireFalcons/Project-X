package com.example.ProjectX.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ProjectX.dto.UserResponseDto;
import com.example.ProjectX.dto.login.UserLoginRequestDto;
import com.example.ProjectX.dto.login.UserLoginResponseDto;
import com.example.ProjectX.dto.register.UserRegistrationRequestDto;
import com.example.ProjectX.dto.register.UserRegistrationResponseDto;
import com.example.ProjectX.model.User;
import com.example.ProjectX.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class UserController {
    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public UserRegistrationResponseDto register(@RequestBody UserRegistrationRequestDto requestDto) {
        return userService.register(requestDto);
    }

    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody UserLoginRequestDto requestDto) {
        return userService.login(requestDto);
    }

    @GetMapping()
    public List<UserResponseDto> getAll(@AuthenticationPrincipal User user) {
        return userService.getAllUsers(user);
    }
}
