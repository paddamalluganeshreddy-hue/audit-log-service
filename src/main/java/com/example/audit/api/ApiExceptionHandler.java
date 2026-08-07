package com.example.audit.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
@RestControllerAdvice public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(IllegalArgumentException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler(ConstraintViolationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> constraintViolation(ConstraintViolationException exception) {
        return Map.of("error", exception.getConstraintViolations().iterator().next().getMessage());
    }
}
