package com.example.ProjectX.exception.file;

import com.example.ProjectX.exception.BadRequestException;

public class InvalidSizeFormatException extends BadRequestException {
    public InvalidSizeFormatException(String message) {
        super(message);
    }
}
