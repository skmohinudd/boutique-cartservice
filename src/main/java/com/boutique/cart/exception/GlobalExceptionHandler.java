package com.boutique.cart.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(),
                "/problems/resource-not-found", request);
    }

    @ExceptionHandler(InvalidCartOperationException.class)
    ProblemDetail handleConflict(InvalidCartOperationException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Invalid cart operation", exception.getMessage(),
                "/problems/invalid-cart-operation", request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ProblemDetail handleDownstream(DownstreamServiceException exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dependent service unavailable",
                exception.getMessage(), "/problems/dependency-unavailable", request);
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleRedis(DataAccessException exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Cart storage unavailable",
                "Redis is unavailable.", "/problems/cart-storage-unavailable", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "Request validation failed.", "/problems/validation-error", request);

        problem.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList()
        );

        return problem;
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String type,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(type));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
