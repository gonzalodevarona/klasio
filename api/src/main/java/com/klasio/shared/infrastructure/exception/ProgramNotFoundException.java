package com.klasio.shared.infrastructure.exception;

public class ProgramNotFoundException extends RuntimeException {

    public ProgramNotFoundException(String message) {
        super(message);
    }
}
