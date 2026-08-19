package com.example.ProjectX.exception.filter;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(@Nullable String msg) {
        super(msg);
    }
}
