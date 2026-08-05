package com.boutique.cart.exception;

public class InvalidCartOperationException extends RuntimeException {

    public InvalidCartOperationException(String message) {
        super(message);
    }
}
