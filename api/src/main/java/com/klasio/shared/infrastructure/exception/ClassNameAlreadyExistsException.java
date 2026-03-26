package com.klasio.shared.infrastructure.exception;

public class ClassNameAlreadyExistsException extends RuntimeException {

    public ClassNameAlreadyExistsException(String message) {
        super(message);
    }
}
