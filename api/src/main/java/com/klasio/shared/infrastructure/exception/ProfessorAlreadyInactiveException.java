package com.klasio.shared.infrastructure.exception;

public class ProfessorAlreadyInactiveException extends RuntimeException {

    public ProfessorAlreadyInactiveException(String message) {
        super(message);
    }
}
