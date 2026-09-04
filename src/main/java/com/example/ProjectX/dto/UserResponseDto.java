package com.example.ProjectX.dto;

import java.util.UUID;

import com.example.ProjectX.model.Role;

public record UserResponseDto(UUID id, String email, Role role) {
}
