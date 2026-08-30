package com.example.ProjectX.exception.login;

import com.example.ProjectX.exception.BadRequestException;

public class PasswordException extends BadRequestException {
    public PasswordException(String message) {
        super(message);
    }
}
