package com.example.ProjectX.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.ProjectX.exception.file.AccessibleRefusedException;
import com.example.ProjectX.exception.file.FileNotFoundException;
import com.example.ProjectX.exception.filter.InvalidTokenException;
import com.example.ProjectX.exception.login.EmailFoundException;
import com.example.ProjectX.exception.login.PasswordException;
import com.example.ProjectX.exception.register.AuthException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice()
public class GlobalExceptionHandler {

    @ExceptionHandler({AuthException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponseDto> handlerUnauthorizedException(RuntimeException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerEmailFound(EmailFoundException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
                       PasswordException.class, 
                       MissingServletRequestParameterException.class,
                       MissingServletRequestPartException.class, 
                       MethodArgumentTypeMismatchException.class, 
                       NoResourceFoundException.class
                    })
    public ResponseEntity<ErrorResponseDto> handlerBedRequestException(Exception ex) {
        String message = switch (ex) {
            case PasswordException p -> "Incorrect password!";
            case MissingServletRequestParameterException e -> "Invalid query parameters!";
            case MissingServletRequestPartException p -> "No file attached to request";
            case MethodArgumentTypeMismatchException m -> "Incorrectly specified id";
            case NoResourceFoundException f -> "Request failed! Cannot access a non-existent request";
            default -> "Something went wrong with request layout";
        };
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({FileNotFoundException.class, AccessibleRefusedException.class})
    public ResponseEntity<ErrorResponseDto> handlerNot_FoundException(RuntimeException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handlerMaxSizeExceededException(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerGenericException(Exception ex) {
        log.error("Something went wrong", ex);  
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    public ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message) {
        ErrorResponseDto body = new ErrorResponseDto(LocalDateTime.now(), status.value(), message);
        return ResponseEntity.status(status).body(body);
    }
}
