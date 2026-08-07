package com.example.audit.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
@RestControllerAdvice public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(IllegalArgumentException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler(ConstraintViolationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> constraintViolation(ConstraintViolationException exception) {
        return Map.of("error", exception.getConstraintViolations().iterator().next().getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalidRequest(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(
                org.springframework.validation.FieldError::getField,
                error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                (first, ignored) -> first, LinkedHashMap::new));
        return Map.of("error", "Validation failed", "fieldErrors", fields);
    }
}
