package com.klasio.shared.infrastructure.exception;

public class ProfessorEmailAlreadyExistsException extends RuntimeException {

    public ProfessorEmailAlreadyExistsException(String message) {
        super(message);
    }
}
