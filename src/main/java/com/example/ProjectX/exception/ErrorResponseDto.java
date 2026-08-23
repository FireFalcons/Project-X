package com.example.ProjectX.exception;

import java.time.LocalDateTime;

public record ErrorResponseDto (LocalDateTime timestamp, int status, String message) {}
